package com.fijimf.deepfij.models.services

import java.time.{LocalDate, LocalDateTime}
import javax.inject.Inject

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.scraping.ScoreboardByDateReq
import com.fijimf.deepfij.scraping.modules.scraping.EmptyBodyException
import com.fijimf.deepfij.scraping.modules.scraping.model.{GameData, ResultData}
import com.google.inject.name.Named
import controllers._
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer.{Email, MailerClient}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class ScheduleUpdateServiceImpl @Inject()(dao: ScheduleDAO, mailerClient: MailerClient, override val messagesApi: MessagesApi, @Named("data-load-actor") teamLoad: ActorRef, @Named("throttler") throttler: ActorRef) extends ScheduleUpdateService with I18nSupport {
  val logger = Logger(this.getClass)

  implicit val timeout = Timeout(600.seconds)
  val activeYear = 2017

  def update(optOffsets: Option[List[Int]] = None, mailReport: Boolean = false) {
    val now = LocalDate.now()
    val optDates = optOffsets.map(_.map(i=>now.plusDays(i)))
    dao.findSeasonByYear(activeYear).map {
      case None =>
        scheduleNotFound(optDates, mailReport)
      case Some(s) =>
        updateSeason(optDates, s, mailReport)
    }
  }

  def updateSeason(optDates: Option[List[LocalDate]], s: Season, mailReport: Boolean) = {
    logger.info(s"Updating season ${s.year} for ${optDates.map(_.mkString(",")).getOrElse("all dates")}.")
    if (dao.checkAndSetLock(s.id)) {
      val updatedBy: String = "Scraper[Updater]"
      scrapeSeasonGames(s, optDates, updatedBy).onComplete {
        case Success(_) =>
          logger.info("Schedule update scrape complete.")
          if (mailReport) {
            logger.info("Mailing summary")
            mailSuccessReport(optDates)
          }
          dao.unlockSeason(s.id)
        case Failure(thr) =>
          logger.error("Schedule update scrape failed.", thr)
          if (mailReport) {
            logger.info("Mailing summary")
            maillErrorReport(optDates)
          }
          dao.unlockSeason(s.id)
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
      subject = Messages("email.daily.update.subject"),
      from = Messages("email.from"),
      to = Seq(System.getProperty("admin.user", "nope@nope.com")),
      bodyHtml = Some(views.html.admin.emails.dailyUpdate(optDates.map(_.mkString(", ")).getOrElse(s" entire $activeYear season ")).body)
    ))
  }

  private def maillErrorReport(optDates: Option[List[LocalDate]]) = {
    mailerClient.send(Email(
      subject = Messages("email.daily.updateError.subject"),
      from = Messages("email.from"),
      to = Seq(System.getProperty("admin.user", "nope@nope.com")),
      bodyHtml = Some(views.html.admin.emails.dailyUpdateError(optDates.map(_.mkString(", ")).getOrElse(s" entire $activeYear season ")).body)
    ))
  }

  def completeScrape(seasonId: Long): Future[Int] = {
    dao.unlockSeason(seasonId)
  }


  def updateDb(oldGameList: List[(Game, Option[Result])], updateData: List[GameMapping]): Future[Unit] = {
    val gameToGameIdMap: Map[GameSignature, Game] = oldGameList.map(t => t._1.signature-> t._1).toMap
    val gameIdToResultIdMap: Map[Long, Result] = oldGameList.flatMap(t => t._2.map(u => t._1.id -> u)).toMap

    val (remainingGames, remainingResults) = updateData.foldLeft((gameToGameIdMap, gameIdToResultIdMap))((tuple: (Map[GameSignature, Game], Map[Long, Result]), mapping: GameMapping) => {
      val (gIdMap, rIdMap) = tuple
      mapping match {
        case MappedGame(g: Game) => handleMappedGame(gIdMap, rIdMap, g)
        case MappedGameAndResult(g: Game, r: Result) => handleMappedGameAndResult(gIdMap, rIdMap, g, r)
        case UnmappedGame(keys: List[String]) => {
          logger.info(s"Failed to map ${keys.mkString(", ")}")
          (gIdMap, rIdMap)
        }
      }
    })
    val resultsToDelete = remainingResults.values.map(_.id).toList
    val gamesToDelete = remainingGames.values.map(_.id).toList
    dao.deleteResultsByGameId(resultsToDelete).flatMap(_ => {
      dao.deleteGames(gamesToDelete)
    })
  }

  private def handleMappedGameAndResult(gIdMap: Map[GameSignature, Game], rIdMap: Map[Long, Result], g: Game, r: Result) = {
    gIdMap.get(g.signature) match {
      case Some(h) if h.sameData(g) => //This is an existing game with no changes
        handleResult(gIdMap-g.signature, rIdMap, g, r, h)
      case Some(h) => //This is an existing game to be updated
        dao.upsertGame(g.copy(id = h.id))
        handleResult(gIdMap-g.signature, rIdMap, g,r,h)
      case None => //This is a new game
        dao.upsertGame(g).flatMap(gameId => dao.upsertResult(r.copy(gameId = gameId)))
        (gIdMap, rIdMap)
    }
  }

  private def handleResult(gIdMapRv: Map[GameSignature, Game], rIdMap: Map[Long, Result], g: Game, r: Result, h: Game) = {
    val r1 = r.copy(gameId = h.id)
    val hIdMap = rIdMap.get(h.id) match {
      case Some(s) if s.sameData(r1) => //This is an existing result with no changes
        rIdMap - h.id
      case Some(s) => //This is an existing result to be updated
        dao.upsertResult(r1.copy(id = s.id))
        rIdMap - h.id
      case None => //This is a new result
        dao.upsertResult(r1)
        rIdMap
    }
    (gIdMapRv,hIdMap)
  }

  private def handleMappedGame(gIdMap: Map[GameSignature, Game], rIdMap: Map[Long, Result], g: Game) = {
    val fIdMap = gIdMap.get(g.signature) match {
      case Some(h) if h.sameData(g) => //This is an existing game with no changes
        gIdMap - g.signature
      case Some(h) => //This is an existing game to be updated
        dao.upsertGame(g.copy(id = h.id))
        gIdMap - g.signature
      case None => //This is a new game
        dao.upsertGame(g)
        gIdMap
    }
    (fIdMap, rIdMap)
  }

  def scrapeSeasonGames(season: Season, optDates: Option[List[LocalDate]], updatedBy: String): Future[Unit] = {
    val dateList: List[LocalDate] = optDates.getOrElse(season.dates).filter(d => season.status.canUpdate(d))
    dao.listAliases.flatMap(aliasDict => {
      dao.listTeams.map(teamDictionary => {
        dateList.foreach(d => {
          for (
            updateData <- scrape(season.id, updatedBy, teamDictionary, aliasDict, d);
            oldGameList <- dao.gamesBySource(d.toString)
          ) yield {
            logger.info(s"For date $d database held ${oldGameList.size} records, scrape resulted in ${updateData.size} candidates.  Verifying changes.")
            updateDb(oldGameList, updateData)
          }
        })
      })

    })
  }

  def gameDataDates(gds: List[GameMapping]): List[LocalDate] = {
    gds.flatMap {
      case MappedGame(g) => Some(g.date)
      case MappedGameAndResult(g, r) => Some(g.date)
      case UnmappedGame(_) => None
    }.distinct
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
      case Right(lgd) => lgd.map(gameDataToGame(seasonId, updatedBy, masterDict, _))
    }
  }


  private def createMasterDictionary(teams: List[Team], aliases: List[Alias]) = {
    val teamDict = teams.map(t => t.key -> t).toMap
    val aliasDict = aliases.filter(a => teamDict.contains(a.key)).map(a => a.alias -> teamDict(a.key))

    val masterDict = teamDict ++ aliasDict
    masterDict
  }

  def gameDataToGame(seasonId: Long, updatedBy: String, teamDict: Map[String, Team], gd: GameData): GameMapping = {
    val atk = gd.awayTeamKey
    val htk = gd.homeTeamKey
    (teamDict.get(htk), teamDict.get(atk)) match {
      case (None, None) => UnmappedGame(List(htk, atk))
      case (Some(t), None) => UnmappedGame(List(atk))
      case (None, Some(t)) => UnmappedGame(List(htk))
      case (Some(ht), Some(at)) => {
        val game = populateGame(seasonId, updatedBy, gd, ht, at)
        gd.result match {
          case Some(rd) => MappedGameAndResult(game, populateResult(updatedBy, rd))
          case None => MappedGame(game)
        }
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

}