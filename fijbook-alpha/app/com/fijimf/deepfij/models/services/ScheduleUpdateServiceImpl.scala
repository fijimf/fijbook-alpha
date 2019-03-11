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
import scala.util.matching.Regex
import scala.util.{Failure, Success}


sealed trait ScheduleUpdateControl {
  def season: Int

  def dates: Option[List[LocalDate]]

  def dateOK(d: LocalDate): Boolean = {
    !d.isBefore(Season.startDate(season)) && !d.isAfter(Season.endDate(season))
  }
}

case class WholeSeasonUpdate(season: Int) extends ScheduleUpdateControl {
  val dates = Option.empty[List[LocalDate]]
}

case class ToFromTodayUpdate(season: Int, daysBack: Int, daysAhead:Int) extends ScheduleUpdateControl {
  val today: LocalDate = LocalDate.now()
  val dates = Some((-1 * daysBack).to(daysAhead).map(today.plusDays(_)).filter(dateOK).toList)
}

case class SingleDateUpdate(season: Int, d: LocalDate) extends ScheduleUpdateControl {
  override def dates: Option[List[LocalDate]] = Some(List(d).filter(dateOK))
}

object UpdateControlString {
  val wholeSeason: Regex = """(\d{4})\.(?i)(all)""".r
  val seasonBeforeAndAfterNow: Regex = """(\d{4})\.(\d+)\.(d+)""".r
  val seasonDate: Regex = """(\d{4})\.(\d{8})""".r

  def apply(str: String): ScheduleUpdateControl = str match {
    case wholeSeason(s, _) => WholeSeasonUpdate(s.toInt)
    case seasonBeforeAndAfterNow(s, b, a) => ToFromTodayUpdate(s.toInt, b.toInt, a.toInt)
    case seasonDate(s, d) => SingleDateUpdate(s.toInt, LocalDate.parse(d, DateTimeFormatter.ofPattern("yyyyMMdd")))
  }
}


class ScheduleUpdateServiceImpl @Inject()(dao: ScheduleDAO, override val messagesApi: MessagesApi, @Named("data-load-actor") teamLoad: ActorRef, @Named("throttler") throttler: ActorRef)(implicit ec: ExecutionContext) extends ScheduleUpdateService with I18nSupport {
  val logger = Logger(this.getClass)

  val zoneId: ZoneId = ZoneId.of("America/New_York")
  implicit val timeout: Timeout = Timeout(600.seconds)

  def update(str: String): Future[String] = {
    val control = UpdateControlString(str)
    dao.findSeasonByYear(control.season).flatMap {
      case Some(s) => updateSeason(control.dates, s).map(collapseResults(s.year, _))
      case None => Future(s"No schedule found for ${control.season}")
    }
  }

  def collapseResults(y: Int,rs:List[UpdateDbResult]):String={
    val zero = UpdateDbResult("y", Seq.empty[Long], Seq.empty[Long], Seq.empty[Long], Seq.empty[Long])
    rs.foldLeft(zero) { case (u, u1) => u.merge(y.toString, u1) }.toString
  }

  def loadSeason(s: Season, tag: String): Future[List[UpdateDbResult]] = {
    val f = scrapeSeasonGames(s, Some(s.dates), tag)
    f.onComplete {
      case Success(_) => logger.info(s"Game scrape succeeded for season ${s.year}")
      case Failure(_) => logger.info(s"Game scrape failed for season ${s.year}")
    }
    f
  }

  def updateSeason(optDates: Option[List[LocalDate]]): Future[List[UpdateDbResult]] = {
    dao.listSeasons.flatMap(ss => {
      runSeasonDates(optDates, ss)
    })
  }

  private def runSeasonDates(optDates: Option[List[LocalDate]], ss: List[Season]): Future[List[UpdateDbResult]] = {
    (optDates match {
      case Some(ds) =>
        val eventualResultses = ds.groupBy(d => ss.find(s => s.dates.contains(d)))
          .filter { case (ms: Option[Season], dates: List[LocalDate]) => ms.isDefined && dates.nonEmpty }
          .map { case (ms: Option[Season], dates: List[LocalDate]) => updateSeason(Some(dates), ms.get) }
        Future.sequence(eventualResultses).map(_.flatten)
      case None => updateSeason(None, ss.maxBy(_.year))
    }).map(_.toList)
  }

  def updateSeason(optDates: Option[List[LocalDate]], s: Season): Future[List[UpdateDbResult]] = {
    logger.info(s"Updating season ${s.year} for ${optDates.map(_.mkString(",")).getOrElse("all dates")}.")
    val updatedBy: String = "Scraper[Updater]"
    val results: Future[List[UpdateDbResult]] = scrapeSeasonGames(s, optDates, updatedBy)
    results.onComplete {
      case Success(_) =>
        logger.info("Schedule update scrape complete.")
      case Failure(thr) =>
        logger.error("Schedule update scrape failed.", thr)
    }
    results
  }

  def updateDb(keys: List[String], updateData: List[GameMapping]): Future[Iterable[UpdateDbResult]] = {
    val groups = updateData.groupBy(_.sourceKey)
    val eventualTuples = keys.map(k => {
      val gameMappings = groups.getOrElse(k, List.empty[GameMapping])
      dao.updateScoreboard(gameMappings, k)
    })
    Future.sequence(eventualTuples)
  }


  def scrapeSeasonGames(season: Season, optDates: Option[List[LocalDate]], updatedBy: String): Future[List[UpdateDbResult]] = {
    val dates: List[LocalDate] = optDates match {
      case Some(ds)=>ds.filter(season.canUpdate)
      case None=> season.dates
    }

    for {
      aliases<-dao.listAliases
      teams<-dao.listTeams
      data<-loadGameData(dates,season,updatedBy,teams,aliases)
    } yield {
      data
    }
  }

  def loadGameData(dates:List[LocalDate],season:Season, updatedBy:String, teams:List[Team], aliases:List[Alias] ): Future[List[UpdateDbResult]] =
    Future.sequence(dates.map({loadOneDate(season, updatedBy, teams, aliases, _)})).map(_.flatten)


  def loadOneDate(season: Season, updatedBy: String, teams: List[Team], aliases: List[Alias], d: LocalDate): Future[Iterable[UpdateDbResult]] = {
    for {
      updateData <- scrape(season, updatedBy, teams, aliases, d)
      updateResults <- updateDb(List(d.toString), updateData)
    } yield {
      updateResults
    }
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