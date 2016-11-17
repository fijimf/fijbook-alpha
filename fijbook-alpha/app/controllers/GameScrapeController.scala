package controllers

import java.time.{LocalDate, LocalDateTime}

import akka.actor.ActorRef
import akka.contrib.throttle.Throttler
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models.{Game, ScheduleDAO, Team, Result => GameResult}
import com.fijimf.deepfij.scraping.ScoreboardByDateReq
import com.fijimf.deepfij.scraping.modules.scraping.model.{GameData, ResultData}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class GameScrapeController @Inject()(@Named("data-load-actor") teamLoad: ActorRef, @Named("throttler") throttler: ActorRef, val scheduleDao: ScheduleDAO, silhouette: Silhouette[DefaultEnv]) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)
  implicit val timeout = Timeout(600.seconds)

  def scrapeGames(seasonId: Long) = silhouette.SecuredAction.async { implicit rs =>
    if (scheduleDao.checkAndSetLock(seasonId)) {
      logger.info("Setting throttler")
      throttler ! Throttler.SetTarget(Some(teamLoad))
      logger.info("Scraping season")
      val updatedBy: String = "Scraper[" + rs.identity.userID.toString + "]"

      scheduleDao.findSeasonById(seasonId).map {
        case Some(season) => {
          logger.info("Found season " + season)
          scheduleDao.listTeams.map(teamDictionary => {
            logger.info("Loaded team dictionary")
            val dateList: List[LocalDate] = season.dates.filter(d => season.status.canUpdate(d))
            Future.sequence(dateList.map(d => scrape(seasonId, updatedBy, teamDictionary, d))).onComplete(_ => completeScrape(seasonId))
          })
          Redirect(routes.AdminController.index()).flashing("info"->("Scraping game data for "+seasonId))
        }
        case None => Redirect(routes.AdminController.index()).flashing("error" -> "Season was not found.  Unable to scrape")
      }

    } else {
      Future.successful(Redirect(routes.AdminController.index()).flashing("error" -> "Season was not found or was locked.  Unable to scrape"))
    }
  }

  def completeScrape(seasonId: Long): Future[Int] = {
    logger.info("Completed scraping dates.")
    scheduleDao.unlockSeason(seasonId)
  }

  def scrape(seasonId: Long, updatedBy: String, teams: List[Team], d: LocalDate): Future[List[Long]] = {
    val teamDict = teams.map(t => t.key -> t).toMap
    logger.info("Loading date " + d)
    val flGameData: Future[List[GameData]] = (throttler ? ScoreboardByDateReq(d)).mapTo[List[GameData]]
    val flGameResults: Future[List[Option[(Game, Option[GameResult])]]] = flGameData.map(_.map(gameDataToGame(seasonId, updatedBy, teamDict, _)))
    flGameResults.flatMap(l => {
      val sequence: Future[List[Long]] = Future.sequence(l.flatten.map(scheduleDao.saveGame))
      sequence.onComplete {
        case Success(ll)=>logger.info("For "+d+" scraped "+ll.size+" games.")
        case Failure(thr)=>logger.error("For "+d+" failed.",thr)
      }
      sequence
    })
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