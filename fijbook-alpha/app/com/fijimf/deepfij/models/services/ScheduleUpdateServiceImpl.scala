package com.fijimf.deepfij.models.services

import java.time.{LocalDate, LocalDateTime}
import javax.inject.Inject

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.scraping.modules.scraping.model.{GameData, ResultData}
import com.fijimf.deepfij.scraping.{EmptyBodyException, SagarinRequest, SagarinRow, ScoreboardByDateReq}
import com.google.inject.name.Named
import controllers._
import play.api.Logger
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.libs.mailer.{Email, MailerClient}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class ScheduleUpdateServiceImpl @Inject()(dao: ScheduleDAO, mailerClient: MailerClient, override val messagesApi: MessagesApi, @Named("data-load-actor") teamLoad: ActorRef, @Named("throttler") throttler: ActorRef)(implicit ec: ExecutionContext) extends ScheduleUpdateService with I18nSupport {
  val logger = Logger(this.getClass)

  implicit val timeout = Timeout(600.seconds)
  val activeYear = 2017

  def update(optOffsets: Option[List[Int]] = None, mailReport: Boolean = false) {
    val now = LocalDate.now()
    val optDates = optOffsets.map(_.map(i => now.plusDays(i)))
    dao.findSeasonByYear(activeYear).map {
      case None =>
        scheduleNotFound(optDates, mailReport)
      case Some(s) =>
        updateSeason(optDates, s, mailReport)
    }
  }

  def updateSeason(optDates: Option[List[LocalDate]], mailReport: Boolean) = {
    dao.listSeasons.map(ss => {
      optDates match {
        case Some(ds) =>
          ds.groupBy(d => ss.find(s => s.dates.contains(d)))
            .filter { case (ms: Option[Season], dates: List[LocalDate]) => ms.isDefined && dates.nonEmpty }
            .foreach { case (ms: Option[Season], dates: List[LocalDate]) => updateSeason(Some(dates), ms.get, mailReport) }
        case None => updateSeason(None, ss.maxBy(_.year), mailReport)
      }
    })
  }

  def updateSeason(optDates: Option[List[LocalDate]], s: Season, mailReport: Boolean): Unit = {
    logger.info(s"Updating season ${s.year} for ${optDates.map(_.mkString(",")).getOrElse("all dates")}.")
    val updatedBy: String = "Scraper[Updater]"
    scrapeSeasonGames(s, optDates, updatedBy).onComplete {
      case Success(_) =>
        logger.info("Schedule update scrape complete.")
        if (mailReport) {
          logger.info("Mailing summary")
          mailSuccessReport(optDates)
        }
      case Failure(thr) =>
        logger.error("Schedule update scrape failed.", thr)
        if (mailReport) {
          logger.info("Mailing summary")
          maillErrorReport(optDates)
        }
    }
  }

  private def scheduleNotFound(optDates: Option[List[LocalDate]], mailReport: Boolean): Unit = {
    logger.warn(s"Schedule not found for year $activeYear. Cannot run update")
    if (mailReport) {
      maillErrorReport(optDates)
    }
  }

  private def mailSuccessReport(optDates: Option[List[LocalDate]]) = {
    mailerClient.send(Email(
      subject = ms("email.daily.update.subject"),
      from = ms("email.from"),
      to = Seq(System.getProperty("admin.user", "nope@nope.com")),
      bodyHtml = Some(views.html.admin.emails.dailyUpdate(optDates.map(_.mkString(", ")).getOrElse(s" entire $activeYear season ")).body)
    ))
  }

  implicit def ms: Messages = {
    messagesApi.preferred(Seq(Lang.defaultLang))
  }

  private def maillErrorReport(optDates: Option[List[LocalDate]]) = {
    mailerClient.send(Email(
      subject = ms("email.daily.updateError.subject"),
      from = ms("email.from"),
      to = Seq(System.getProperty("admin.user", "nope@nope.com")),
      bodyHtml = Some(views.html.admin.emails.dailyUpdateError(optDates.map(_.mkString(", ")).getOrElse(s" entire $activeYear season ")).body)
    ))
  }

  def completeScrape(seasonId: Long): Future[Int] = {
    dao.unlockSeason(seasonId)
  }

  def updateDb(keys: List[String], updateData: List[GameMapping]): Future[Iterable[(Seq[Long], Seq[Long])]] = {
    val groups = updateData.groupBy(_.sourceKey)
    Future.sequence(keys.map(sourceKey => dao.updateScoreboard(groups.getOrElse(sourceKey, List.empty[GameMapping]), sourceKey)))
  }

  def scrapeSeasonGames(season: Season, optDates: Option[List[LocalDate]], updatedBy: String): Future[Unit] = {
    val dateList: List[LocalDate] = optDates.getOrElse(season.dates).filter(d => season.status.canUpdate(d))
    dao.listAliases.flatMap(aliasDict => {
      dao.listTeams.map(teamDictionary => {
        dateList.foreach(d => {
          for {
            updateData <- scrape(season.id, updatedBy, teamDictionary, aliasDict, d)
          } yield {
            logger.info(s"For date $d scrape resulted in ${updateData.size} candidates.  Verifying changes.")
            updateDb(List(d.toString), updateData)
          }
        })
      })
    })
  }

  def scrape(seasonId: Long, updatedBy: String, teams: List[Team], aliases: List[Alias], d: LocalDate): Future[List[GameMapping]] = {
    val masterDict: Map[String, Team] = createMasterDictionary(teams, aliases)
    logger.info("Loading date " + d)
    (throttler ? ScoreboardByDateReq(d)).mapTo[Either[Throwable, List[GameData]]].map {
      case Left(thr) if thr == EmptyBodyException =>
        logger.warn("For date " + d + " scraper returned an empty body")
        List.empty[GameMapping]
      case Left(thr) =>
        logger.error("For date " + d + " scraper returned an exception ", thr)
        List.empty[GameMapping]
      case Right(lgd) => lgd.map(gameDataToGame(seasonId, d.toString, updatedBy, masterDict, _))
    }
  }

  private def createMasterDictionary(): Future[Map[String, Team]] = {
    dao.listAliases.flatMap(aliases => {
      dao.listTeams.map(teams => {
        createMasterDictionary(teams, aliases)
      })
    })
  }

  private def createMasterDictionary(teams: List[Team], aliases: List[Alias]): Map[String, Team] = {
    val teamDict = teams.map(t => t.key -> t).toMap
    val aliasDict = aliases.filter(a => teamDict.contains(a.key)).map(a => a.alias -> teamDict(a.key))
    teamDict ++ aliasDict
  }

  def gameDataToGame(seasonId: Long, sourceKey: String, updatedBy: String, teamDict: Map[String, Team], gd: GameData): GameMapping = {
    val atk = gd.awayTeamKey
    val htk = gd.homeTeamKey
    (teamDict.get(htk), teamDict.get(atk)) match {
      case (None, None) => UnmappedGame(List(htk, atk), sourceKey)
      case (Some(t), None) => UnmappedGame(List(atk), sourceKey)
      case (None, Some(t)) => UnmappedGame(List(htk), sourceKey)
      case (Some(ht), Some(at)) =>
        val game = populateGame(seasonId, updatedBy, gd, ht, at)
        gd.result match {
          case Some(rd) => MappedGameAndResult(game, populateResult(updatedBy, rd))
          case None => MappedGame(game)
        }
    }
  }


  def populateResult(updatedBy: String, r: ResultData): Result = {
    Result(
      id = 0L,
      gameId = 0L,
      homeScore = r.homeScore,
      awayScore = r.awayScore,
      periods = r.periods,
      updatedAt = LocalDateTime.now(),
      updatedBy = updatedBy)
  }

  def populateGame(seasonId: Long, updatedBy: String, gd: GameData, ht: Team, at: Team): Game = {
    Game(
      id = 0L,
      seasonId = seasonId,
      homeTeamId = ht.id,
      awayTeamId = at.id,
      date = gd.date.toLocalDate,
      datetime = gd.date,
      location = gd.location,
      isNeutralSite = false,
      tourneyKey = gd.tourneyInfo.map(_.region),
      homeTeamSeed = gd.tourneyInfo.map(_.homeTeamSeed),
      awayTeamSeed = gd.tourneyInfo.map(_.awayTeamSeed),
      sourceKey = gd.sourceKey,
      updatedAt = LocalDateTime.now(),
      updatedBy = updatedBy
    )
  }

  override def verifyRecords(y: Int) = {
    dao.loadSchedule(y).flatMap {
      case Some(sch) =>
        createMasterDictionary().flatMap(md => {
          loadVerification(y, md, sch)
        })
      case None => {
        logger.error("Failed to load ")
        Future.successful(ResultsVerification())
      }
    }
  }


  private def loadVerification(y: Int, md:Map[String,Team], sch:Schedule): Future[ResultsVerification] = {
    (throttler ? SagarinRequest(y)).mapTo[Either[Throwable, List[SagarinRow]]].map {
      case Left(thr) if thr == EmptyBodyException =>
        logger.warn("For year " + y + " Sagarin scraper returned an empty body")
        ResultsVerification()
      case Left(thr) =>
        logger.error("For year " + y + " Sagarin scraper returned an exception ", thr)
        ResultsVerification()
      case Right(lsr) =>
        lsr.foldLeft(ResultsVerification())((v: ResultsVerification, row: SagarinRow) => {
          val key = transformNameToKey(row.sagarinName)
          md.get(key) match {
            case Some(t)=>
              val record = sch.overallRecord(t)
              if (record.won==row.wins && record.lost==row.losses){
                v.copy(matchedResults=t::v.matchedResults)
              } else {
                v.copy(unmatchedResults=t::v.unmatchedResults)
              }
            case None=> v.copy(unmappedKeys = key::v.unmappedKeys)
          }
        })

    }
  }
   def transformNameToKey(n:String):String={
     n.trim()
       .toLowerCase()
       .replace(' ','-')
       .replace('(','-')
       .replaceAll("[\\.&'\\)]","")
   }
}

case class ResultsVerification(unmappedKeys:List[String] = List.empty,matchedResults:List[Team]= List.empty, unmatchedResults:List[Team]=List.empty)