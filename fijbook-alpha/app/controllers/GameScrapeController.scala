package controllers

import java.time.{LocalDate, LocalDateTime}

import akka.actor.ActorRef
import akka.contrib.throttle.Throttler
import akka.pattern._
import akka.util.Timeout
import com.fijimf.deepfij.models.{Game, ScheduleDAO, Season, Team, Result => GameResult}
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
import scala.concurrent.{Await, Future, Promise}
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
          case Success(ll) => logger.info(ll.map(_.mkString(", ")).mkString("\n"))
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


  def scrapeSeasonGames(season: Season, updatedBy: String): Future[List[List[Long]]] = {
    logger.info("Found season " + season)
    scheduleDao.listTeams.flatMap(teamDictionary => {
      logger.info("Loaded team dictionary")
      val dateList: List[LocalDate] = season.dates.filter(d => season.status.canUpdate(d))
      logger.info("Loading " + dateList.size + " dates")
      Await.result(Future.sequence(dateList.map(dd => scheduleDao.clearGamesByDate(dd))), 15.seconds)
      val map: List[Future[List[Long]]] = dateList.map(d => scrape(season.id, updatedBy, teamDictionary, d))
      Future.sequence(map)
    })
  }

  def completeScrape(seasonId: Long): Future[Int] = {
    scheduleDao.unlockSeason(seasonId)
  }

  def scrape(seasonId: Long, updatedBy: String, teams: List[Team], d: LocalDate): Future[List[Long]] = {
    val teamDict = teams.map(t => t.key -> t).toMap
    logger.info("Loading date " + d)
    val future: Future[Either[Throwable, List[GameData]]] = (throttler ? ScoreboardByDateReq(d)).mapTo[Either[Throwable, List[GameData]]]
    val results: Future[List[(Game, Option[GameResult])]] = future.map(_.fold(
      thr => {
        if (thr == EmptyBodyException) {
          logger.warn("For date " + d + " scraper returned an empty body")
        } else {
          logger.error("For date " + d + " scraper returned an exception ", thr)
        }
        List.empty[(Game, Option[GameResult])]
      },
      lgd => {
        lgd.flatMap(gameDataToGame(seasonId, updatedBy, teamDict, _))
      }))
    for (gameList <- results;
         keys <- Future.sequence(gameList.map(scheduleDao.saveGame))
    ) yield {
      keys
    }
  }


  def gameDataToGame(seasonId: Long, updatedBy: String, teamDict: Map[String, Team], gd: GameData): Option[(Game, Option[GameResult])] = {
    for (
      ht <- teamDict.get(gd.homeTeamKey);
      at <- teamDict.get(gd.awayTeamKey)
    ) yield {
      populateGame(seasonId, updatedBy, gd, ht, at) -> gd.result.map(r => populateResult(updatedBy, r))
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