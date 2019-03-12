package com.fijimf.deepfij.models.services

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZoneId}

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.scraping._
import com.fijimf.deepfij.scraping.modules.scraping.model.{GameData, ResultData}
import com.google.inject.name.Named
import controllers._
import javax.inject.Inject
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}


class ScheduleUpdateServiceImpl @Inject()(dao: ScheduleDAO, override val messagesApi: MessagesApi, @Named("data-load-actor") teamLoad: ActorRef, @Named("throttler") throttler: ActorRef)(implicit ec: ExecutionContext) extends ScheduleUpdateService with I18nSupport {
  val logger = Logger(this.getClass)

  val zoneId: ZoneId = ZoneId.of("America/New_York")
  implicit val timeout: Timeout = Timeout(600.seconds)

  def update(str: String): Future[ScheduleUpdateResult] = {
    update(ScheduleUpdateControl(str))
  }

  def update(suc:ScheduleUpdateRequest):Future[ScheduleUpdateResult]={
    dao.findSeasonByYear(suc.season).flatMap {
      case Some(s) => scrapeSeasonGames(s, suc.dates, "Scraper[Updater]").map(collapseResults(s.year, _))
      case None => Future.failed(new RuntimeException(s"No schedule found for ${suc.season}"))
    }
  }

  def scrapeSeasonGames(season: Season, optDates: Option[List[LocalDate]], updatedBy: String): Future[List[ScheduleUpdateResult]] = {
    for {
      before<-dao.loadSchedule(season.year).map(_.map(_.snapshot))
      aliases<-dao.listAliases
      teams<-dao.listTeams
      data<-loadGameData(datesToRun(season, optDates),season,updatedBy,teams,aliases)
      after<-dao.loadSchedule(season.year).map(_.map(_.snapshot))
    } yield {
      println(before)
      println(after)
      data
    }
  }


  private def datesToRun(season: Season, optDates: Option[List[LocalDate]]) = {
    optDates match {
      case Some(ds) => ds.filter(season.canUpdate)
      case None => season.dates
    }
  }

  def collapseResults(y: Int, rs:List[ScheduleUpdateResult]):ScheduleUpdateResult={
    val zero = ScheduleUpdateResult(s"$y", Seq.empty[Long], Seq.empty[Long], Seq.empty[Long], Seq.empty[Long])
    rs.foldLeft(zero) { case (u, u1) => u.merge(y.toString, u1) }
  }



  def loadGameData(dates:List[LocalDate],season:Season, updatedBy:String, teams:List[Team], aliases:List[Alias] ): Future[List[ScheduleUpdateResult]] =
    Future.sequence(dates.map({loadOneDate(season, updatedBy, teams, aliases, _)})).map(_.flatten)


  def loadOneDate(season: Season, updatedBy: String, teams: List[Team], aliases: List[Alias], d: LocalDate): Future[Iterable[ScheduleUpdateResult]] = {
    for {
      updateData <- scrape(season, updatedBy, teams, aliases, d)
      updateResults <- updateDb(List(d.toString), updateData)
    } yield {
      updateResults
    }
  }

  def updateDb(keys: List[String], updateData: List[GameMapping]): Future[Iterable[ScheduleUpdateResult]] = {
    val groups = updateData.groupBy(_.sourceKey)
    Future.sequence(keys.map(k => {
      val gameMappings = groups.getOrElse(k, List.empty[GameMapping])
      dao.updateScoreboard(gameMappings, k)
    }))
  }

  def scrape(season: Season, updatedBy: String, teams: List[Team], aliases: List[Alias], d: LocalDate): Future[List[GameMapping]] = {
    val masterDict: Map[String, Team] = createMasterDictionary(teams, aliases)
    logger.info("Loading date " + d)

    val response = if (season.year>2017){
      (throttler ? CasablancaScoreboardByDateReq(d)).mapTo[ScrapingResponse[List[GameData]]]
    } else {
      (throttler ? ScoreboardByDateReq(d)).mapTo[ScrapingResponse[List[GameData]]]
    }
    logScrapeResponse(d, response)

    response.map(_.result match {
      case Success(lgd) => lgd.map(gameDataToGame(season, d, updatedBy, masterDict, _))
      case Failure(_) => List.empty[GameMapping]
    })
  }

  private def logScrapeResponse(d: LocalDate, futScrapeResp: Future[ScrapingResponse[List[GameData]]]): Unit = {
    futScrapeResp.onComplete {
      case Success(resp) =>
        val dt = d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val u = resp.url
        val lat = "%8d ms".format(resp.latencyMs)
        val sz = "%10d".format(resp.length)
        resp.result match {
          case Success(lgd) =>
            logger.info(s"\n$dt | $u | ${resp.status} | $lat | $sz | ${lgd.size} games")
          case Failure(thr) =>
            logger.info(s"\n$dt | $u | ${resp.status} | $lat | $sz | ${thr.getMessage}")
        }
      case Failure(thr) =>
        logger.error(s"$d | FAILURE- ${thr.getMessage}")
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

  def gameDataToGame(season: Season, d: LocalDate, updatedBy: String, teamDict: Map[String, Team], gd: GameData): GameMapping = {
    val sourceKey = d.toString
    val atk = gd.awayTeamKey
    val htk = gd.homeTeamKey
    (teamDict.get(htk), teamDict.get(atk)) match {
      case (None, None) => UnmappedGame(List(htk, atk), sourceKey)
      case (Some(_), None) => UnmappedGame(List(atk), sourceKey)
      case (None, Some(_)) => UnmappedGame(List(htk), sourceKey)
      case (Some(ht), Some(at)) =>
        val game = populateGame(d, season, updatedBy, gd, ht, at)
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

  def populateGame(d: LocalDate, season: Season, updatedBy: String, gd: GameData, ht: Team, at: Team): Game = {
    val dd = if (gd.date.toLocalDate.isBefore(season.startDate) || gd.date.toLocalDate.isAfter(season.endDate)) {
      d.atStartOfDay
    } else {
      gd.date
    }
    Game(
      id = 0L,
      seasonId = season.id,
      homeTeamId = ht.id,
      awayTeamId = at.id,
      date = dd.toLocalDate,
      datetime = dd,
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

  override def verifyRecords(y: Int): Future[ResultsVerification] = {
    dao.loadSchedule(y).flatMap {
      case Some(sch) =>
        createMasterDictionary().flatMap(md => {
          loadVerification(y, md, sch)
        })
      case None =>
        logger.error("Failed to load ")
        Future.successful(ResultsVerification())
    }
  }


  private def loadVerification(y: Int, md: Map[String, Team], sch: Schedule): Future[ResultsVerification] = {
    (throttler ? SagarinRequest(y)).mapTo[ScrapingResponse[List[SagarinRow]]].map(_.result match {
      case Failure(thr) if thr == EmptyBodyException =>
        logger.warn("For year " + y + " Sagarin scraper returned an empty body")
        ResultsVerification()
      case Success(lsr) =>
        val rv = lsr.foldLeft(ResultsVerification())((v: ResultsVerification, row: SagarinRow) => {
          val key = transformNameToKey(row.sagarinName)
          md.get(key) match {
            case Some(t) =>
              val r = sch.overallRecord(t)
              val s = WonLostRecord(row.wins, row.losses)
              if (r == s) {
                v.copy(matchedResults = t :: v.matchedResults)
              } else {
                v.copy(unmatchedResults = (t, r, s) :: v.unmatchedResults)
              }
            case None => v.copy(unmappedKeys = key :: v.unmappedKeys)
          }
        })
        val found = (rv.unmatchedResults.map(_._1.key) ++ rv.matchedResults.map(_.key)).toSet
        rv.copy(notFound = sch.teams.map(_.key).filterNot(found.contains))
    })
  }

  def transformNameToKey(n: String): String = {
    n.trim()
      .toLowerCase()
      .replace(' ', '-')
      .replace('(', '-')
      .replaceAll("[\\.&'\\)]", "")
  }
}

final case class ResultsVerification(unmappedKeys: List[String] = List.empty, notFound: List[String] = List.empty, matchedResults: List[Team] = List.empty, unmatchedResults: List[(Team, WonLostRecord, WonLostRecord)] = List.empty)