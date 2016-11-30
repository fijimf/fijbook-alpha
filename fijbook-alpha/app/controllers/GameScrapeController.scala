package controllers

import java.time.{LocalDate, LocalDateTime}

import akka.actor.ActorRef
import akka.contrib.throttle.Throttler
import akka.pattern._
import akka.util.Timeout
import com.fijimf.deepfij.models.{Alias, Game, ScheduleDAO, Season, Team, Result => GameResult}
import com.fijimf.deepfij.scraping.ScoreboardByDateReq
import com.fijimf.deepfij.scraping.modules.scraping.EmptyBodyException
import com.fijimf.deepfij.scraping.modules.scraping.model.{GameData, ResultData}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.mvc.{Controller, Result}
import utils.DefaultEnv

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

class GameScrapeController @Inject()(@Named("data-load-actor") teamLoad: ActorRef, @Named("throttler") throttler: ActorRef, val scheduleDao: ScheduleDAO, silhouette: Silhouette[DefaultEnv]) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)
  implicit val timeout = Timeout(600.seconds)
  throttler ! Throttler.SetTarget(Some(teamLoad))

  def scrapeGames(seasonId: Long) = silhouette.SecuredAction.async { implicit rs =>
    val p = Promise[Result]()
    if (scheduleDao.checkAndSetLock(seasonId)) {
      logger.info("Scraping season")
      p.success {
        Redirect(routes.AdminController.index()).flashing("info" -> ("Scraping season " + seasonId))
      }

      val updatedBy: String = "Scraper[" + rs.identity.userID.toString + "]"
      scheduleDao.findSeasonById(seasonId).map {
        case Some(season) => scrapeSeasonGames(season, updatedBy).onComplete {
          case Success(ssr) => {
            ssr.unmappedTeamCount.foreach((tuple: (String, Int)) => if (tuple._2 > 9) println(tuple._1 + "\t" + tuple._2))
          }
            completeScrape(seasonId)
          case Failure(thr) => logger.error("Failed update ", thr)
            completeScrape(seasonId)
        }
        case None => //This should never happen as we've already checked.
      }

    } else {
      p.success {
        Redirect(routes.AdminController.index()).flashing("error" -> "Season was not found or was locked.  Unable to scrape")
      }
    }
    p.future
  }


  def scrapeSeasonGames(season: Season, updatedBy: String): Future[SeasonScrapeResult] = {
    logger.info("Found season " + season)
    scheduleDao.listAliases.flatMap(aliasDict => {
      scheduleDao.listTeams.flatMap(teamDictionary => {
        logger.info("Loaded team dictionary")
        val dateList: List[LocalDate] = season.dates.filter(d => season.status.canUpdate(d))
        logger.info("Loading " + dateList.size + " dates")
        Await.result(Future.sequence(dateList.map(dd => scheduleDao.clearGamesByDate(dd))), 15.seconds)
        val map: List[Future[(LocalDate, GameScrapeResult)]] = dateList.map(d => scrape(season.id, updatedBy, teamDictionary, aliasDict, d).map(d -> _))
        Future.sequence(map).map(lgsr => SeasonScrapeResult(lgsr))
      })
    })
  }

  def completeScrape(seasonId: Long): Future[Int] = {
    scheduleDao.unlockSeason(seasonId)
  }

  def scrape(seasonId: Long, updatedBy: String, teams: List[Team], aliases:List[Alias], d: LocalDate): Future[GameScrapeResult] = {
    val teamDict = teams.map(t => t.key -> t).toMap
    val aliasDict = aliases.filter(a=>teamDict.contains(a.key)).map(a=>a.alias->teamDict(a.key))

    val masterDict = teamDict++aliasDict
    logger.info("Loading date " + d)
    val future: Future[Either[Throwable, List[GameData]]] = (throttler ? ScoreboardByDateReq(d)).mapTo[Either[Throwable, List[GameData]]]
    val results: Future[List[GameMapping]] = future.map(_.fold(
      thr => {
        if (thr == EmptyBodyException) {
          logger.warn("For date " + d + " scraper returned an empty body")
        } else {
          logger.error("For date " + d + " scraper returned an exception ", thr)
        }
        List.empty[GameMapping]
      },
      lgd => lgd.map(gameDataToGame(seasonId, updatedBy, masterDict, _))
    ))
    results.flatMap(gameList => {
      val z = Future.successful(GameScrapeResult())
      gameList.foldLeft(z)((fgsr: Future[GameScrapeResult], mapping: GameMapping) => {
        fgsr.flatMap(_.acc(scheduleDao, mapping))
      })

    })

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


  def populateResult(updatedBy: String, r: ResultData): GameResult = {
    GameResult(
      id = 0L,
      gameId = 0L,
      homeScore = r.homeScore,
      awayScore = r.awayScore,
      periods = r.periods,
      lockRecord = false,
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
      tourneyKey = gd.tourneyInfo.map(_.region),
      homeTeamSeed = gd.tourneyInfo.map(_.homeTeamSeed),
      awayTeamSeed = gd.tourneyInfo.map(_.awayTeamSeed),
      lockRecord = false,
      updatedAt = LocalDateTime.now(),
      updatedBy = updatedBy
    )
  }
}

case class GameScrapeResult(ids: List[Long] = List.empty[Long], unmappedKeys: List[String] = List.empty[String]) {
  def acc(dao: ScheduleDAO, gm: GameMapping)(implicit ec: ExecutionContext): Future[GameScrapeResult] = {
    gm match {
      case UnmappedGame(ss) => Future.successful(copy(unmappedKeys = unmappedKeys ++ ss))
      case MappedGame(g) => dao.saveGame(g -> None).map(id => copy(ids = id :: ids))
      case MappedGameAndResult(g, r) => dao.saveGame(g -> Some(r)).map(id => copy(ids = id :: ids))
    }
  }
}

sealed trait GameMapping

case class MappedGame(g: Game) extends GameMapping

case class MappedGameAndResult(g: Game, r: GameResult) extends GameMapping

case class UnmappedGame(keys: List[String]) extends GameMapping

object SeasonScrapeResult {
  def apply(list: List[(LocalDate, GameScrapeResult)]): SeasonScrapeResult = {
    val gameCounts: Map[LocalDate, Int] = list.map(tup => tup._1 -> tup._2.ids.size).toMap
    val unmappedTeamCount: Map[String, Int] = list.flatMap(_._2.unmappedKeys).groupBy(_.toString).map(tup => (tup._1, tup._2.size))
    SeasonScrapeResult(gameCounts, unmappedTeamCount)
  }
}

case class SeasonScrapeResult(gameCounts: Map[LocalDate, Int], unmappedTeamCount: Map[String, Int])