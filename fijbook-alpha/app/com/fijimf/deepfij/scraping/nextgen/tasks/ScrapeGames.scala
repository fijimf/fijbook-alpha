package com.fijimf.deepfij.scraping.nextgen.tasks

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import akka.actor.{ActorRef, PoisonPill}
import akka.agent.Agent
import akka.contrib.throttle.Throttler
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.scraping.modules.scraping.model.{GameData, ResultData}
import com.fijimf.deepfij.scraping.nextgen.{SSTask, SSTaskProgress}
import com.fijimf.deepfij.scraping.{ScoreboardByDateReq, ScrapingResponse}
import controllers.{GameMapping, MappedGame, MappedGameAndResult, UnmappedGame}
import org.apache.commons.lang3.StringUtils
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class ScrapeGames(dao: ScheduleDAO, throttler:ActorRef) extends SSTask[List[_]] {

  import scala.concurrent.ExecutionContext.Implicits.global

  
  case class UpdateDbResult(source: String, upserted: Seq[Long], deleted: Seq[Long])
  val logger = Logger(this.getClass)
  def name: String = "Scrape games"
  def run(messageListener:Option[ActorRef]):Future[List[_]] = {

    dao.listSeasons.flatMap(ss => {
      val dateCounter = Agent((0, ss.flatMap(_.dates).size))
      Future.sequence(ss.map(s => {
        loadSeason(s, id, messageListener, dateCounter).recover {
          case thr => logger.error(s"Failed loading games for ${s.year} with ${thr.getMessage}")
        }
      }))
    })
  }

  override def cancel = {
    throttler ! Throttler.SetTarget(None)
    throttler ! PoisonPill
  }

  def loadSeason(s: Season, tag: String, messageListener:Option[ActorRef], dateCounter:Agent[(Int, Int)]): Future[List[UpdateDbResult]] = {
    val f = scrapeSeasonGames(s, Some(s.dates), tag, messageListener, dateCounter)
    f.onComplete {
      case Success(_) => logger.info(s"Game scrape succeeded for season ${s.year}")
      case Failure(_) => logger.info(s"Game scrape failed for season ${s.year}")
    }
    f
  }



  def scrapeSeasonGames(season: Season, optDates: Option[List[LocalDate]], tag: String,messageListener:Option[ActorRef], dateCounter:Agent[(Int, Int)]): Future[List[UpdateDbResult]] = {
    val dateList: List[LocalDate] = optDates.getOrElse(season.dates).filter(d => season.status.canUpdate(d))
    dao.listAliases.flatMap(aliasDict => {
      dao.listTeams.flatMap(teamDictionary => {
        Future.sequence(dateList.map(d => {
          val results = for {
            updateData <- scrape(season, tag, teamDictionary, aliasDict, d)
            updateResults <- updateDb(List(d.toString), updateData)
          } yield {
            updateResults
          }
          results.onComplete {
            case Success(x)=> 
              dateCounter.send(tup=>(tup._1+1,tup._2))
              val (num, den) = dateCounter.get
              val pctComplete = num.toDouble/den.toDouble
              messageListener.foreach(_ ! SSTaskProgress(Some(pctComplete), Some(x.map(_.source).mkString(", "))))
            case Failure(thr)=>
              dateCounter.send(tup=>(tup._1+1,tup._2))
              val (num, den) = dateCounter.get
              val pctComplete = num.toDouble/den.toDouble
              messageListener.foreach(_ ! SSTaskProgress(Some(pctComplete), Some(thr.getMessage)))
          }
          results
        })).map(_.flatten)
      })
    })
  }
  def updateDb(keys: List[String], updateData: List[GameMapping]): Future[Iterable[UpdateDbResult]] = {
    val groups = updateData.groupBy(_.sourceKey)
    val eventualTuples = keys.map(k => {
      val gameMappings = groups.getOrElse(k, List.empty[GameMapping])
      dao.updateScoreboard(gameMappings, k).map(tup => UpdateDbResult(k, tup._1, tup._2))
    })
    Future.sequence(eventualTuples)
  }

  def scrape(season: Season, updatedBy: String, teams: List[Team], aliases: List[Alias], d: LocalDate): Future[List[GameMapping]] = {
    val masterDict: Map[String, Team] = createMasterDictionary(teams, aliases)
    logger.info("Loading date " + d)
    implicit val timeout=Timeout(10.minutes)
    val response = (throttler ? ScoreboardByDateReq(d)).mapTo[ScrapingResponse[List[GameData]]]
    logScrapeResponse(d, response)

    response.map(_.result match {
      case Success(lgd) => lgd.map(gameDataToGame(season, d, updatedBy, masterDict, _))
      case Failure(thr) => List.empty[GameMapping]
    })
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
      case (Some(t), None) => UnmappedGame(List(atk), sourceKey)
      case (None, Some(t)) => UnmappedGame(List(htk), sourceKey)
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

  private def logScrapeResponse(d: LocalDate, futScrapeResp: Future[ScrapingResponse[List[GameData]]]): Unit = {
    futScrapeResp.onComplete {
      case Success(resp) =>
        val dt = d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val u = StringUtils.abbreviateMiddle(resp.url, " ... ", 48)
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

  override def safeToRun: Future[Boolean] = Future.successful(true)
}




