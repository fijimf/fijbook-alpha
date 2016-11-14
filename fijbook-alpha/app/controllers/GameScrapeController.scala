package controllers

import java.time.{LocalDate, LocalDateTime}

import akka.actor.{ActorRef, ActorSystem}
import akka.contrib.throttle.Throttler
import akka.contrib.throttle.Throttler._
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.scraping.ScoreboardByDateReq
import com.fijimf.deepfij.scraping.modules.scraping.model.GameData
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future
import scala.concurrent.duration._

class GameScrapeController @Inject()(@Named("data-load-actor") teamLoad: ActorRef,@Named("throttler") throttler: ActorRef, val teamDao: ScheduleDAO, silhouette: Silhouette[DefaultEnv]) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)
  implicit val timeout = Timeout(600.seconds)

  throttler ! new Throttler.SetRate(Throttler.Rate(1, 1.second))
  throttler ! new Throttler.SetTarget(teamLoad)

  //  def scrapeDate = silhouette.SecuredAction.async {
  //    implicit rs =>
  //      OneDateForm.form.bindFromRequest.fold(
  //        form => Future.successful( BadRequest(views.html.admin.index(Some(rs.identity),List.empty, form))),
  //        data => {
  //          val d=data.date
  //          teamDao.findSeason(d).flatMap(scrapeLocalDate(_,d))
  //        }
  //
  //  }
  def scrapeLocalDate(seasonId: Long, date: LocalDate) = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Scraping date " + date)
    val updatedBy: String = "Scraper[" + rs.identity.userID.toString + "]"

    teamDao.findSeasonById(seasonId).map {
      case Some(season) => {
        logger.info("Found sesaon " + season)
        teamDao.listTeams.map(teams => {
          logger.info("Loaded team dictionary")
          scrape(seasonId, updatedBy, teams, date)
        })
      }
    }
    Future.successful(Redirect(routes.AdminController.index()))
  }

  def scrapeOneDay(seasonId: Long, year: Int, month: Int, day: Int) = scrapeLocalDate(seasonId, LocalDate.of(year, month, day))


  def scrapeGames(seasonId: Long) = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Scraping season")
    val updatedBy: String = "Scraper[" + rs.identity.userID.toString + "]"

    teamDao.findSeasonById(seasonId).map {
      case Some(season) => {
        logger.info("Found sesaon " + season)
        teamDao.listTeams.map(teams => {
          logger.info("Loaded team dictionary")

          val dates: List[LocalDate] = season.dates.toList
          dates.map(d => scrape(seasonId, updatedBy, teams, d))
        })
        Redirect(routes.AdminController.index())
      }
      case None => Redirect(routes.AdminController.index()).flashing("error" -> "Season was not found.  Unable to scrape")
    }
  }

  def scrape(seasonId: Long, updatedBy: String, teams: List[Team], d: LocalDate): Future[List[Long]] = {
    val teamDict = teams.map(t => t.key -> t).toMap
    logger.info("Loading date " + d)
    (throttler ? ScoreboardByDateReq(d))
      .mapTo[List[GameData]]
      .map(_.map(gameDataToGame(seasonId, updatedBy, teamDict, _)))
      .flatMap(l => Future.sequence(l.flatten.map(teamDao.saveGame)))


  }

  def gameDataToGame(seasonId: Long, updatedBy: String, teamDict: Map[String, Team], gd: GameData): Option[(Game, Option[Result])] = {
    if (teamDict.contains(gd.homeTeamKey) && teamDict.contains(gd.awayTeamKey)) {
      Some(Game(
        0L,
        seasonId,
        teamDict(gd.homeTeamKey).id,
        teamDict(gd.awayTeamKey).id,
        gd.date,
        gd.location,
        gd.tourneyInfo.map(_.region),
        gd.tourneyInfo.map(_.homeTeamSeed),
        gd.tourneyInfo.map(_.awayTeamSeed),
        false,
        LocalDateTime.now(),
        updatedBy
      ) -> gd.result.map(r => com.fijimf.deepfij.models.Result(
        0,
        0,
        r.homeScore,
        r.awayScore,
        r.periods,
        false,
        LocalDateTime.now(),
        updatedBy))
      )
    } else {
      logger.info("Failed to map game " + gd)
      None
    }
  }

    def scrapeDates() = play.mvc.Results.TODO
}