package controllers

import java.time.{LocalDate, LocalDateTime, LocalTime}

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.scraping.ScoreboardByDateReq
import com.fijimf.deepfij.scraping.modules.scraping.model.GameData
import com.fijimf.deepfij.scraping.modules.scraping.requests.{ShortNameAndKeyByStatAndPage, TeamDetail}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Silhouette
import forms.{EditTeamForm, OneDateForm}
import play.api.Logger
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ScraperController @Inject()(@Named("data-load-actor") teamLoad: ActorRef, val teamDao: TeamDAO, silhouette: Silhouette[DefaultEnv]) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)
  implicit val timeout = Timeout(600.seconds)

  def scrapeTeams() = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Loading preliminary team keys.")
    val teamShortNames: Future[Map[String, String]] = masterShortName(List(1, 2, 3, 4, 5, 6, 7), 145)

    logger.info("Loading team detail")

    val teamMaster: Future[List[Team]] = teamShortNames.map((tsn: Map[String, String]) => {
      tsn.keys.grouped(4).map((is: Iterable[String]) => {
        Await.result(Future.sequence(is.map((k: String) => {
          (teamLoad ? TeamDetail(k, tsn(k), "Scraper[" + rs.identity.userID.toString + "]")).mapTo[Team]
        })), 600.seconds)
      }).flatten.toList
    })

    teamMaster.flatMap(lst => {
      val (good, bad) = lst.partition(t => t.name.trim.nonEmpty && t.nickname.trim.nonEmpty)
      good.foreach(t => {
        logger.info("Saving " + t.key)
        teamDao.save(t)
      })
      Future {
        val badTeamlist: String = bad.map(_.key).mkString("\n")
        logger.info("The following teams were bad:\n" + badTeamlist)
        Ok(badTeamlist)
      }
    })

  }

  def masterShortName(pagination: List[Int], stat: Int): Future[Map[String, String]] = {
    pagination.foldLeft(Future.successful(Seq.empty[(String, String)]))((data: Future[Seq[(String, String)]], p: Int) => {
      for (
        t0 <- data;
        t1 <- (teamLoad ? ShortNameAndKeyByStatAndPage(stat, p)).mapTo[Seq[(String, String)]]
      ) yield t0 ++ t1
    }).map(_.toMap)
  }


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
    logger.info("Scraping date "+ date)
    val updatedBy: String = "Scraper[" + rs.identity.userID.toString + "]"

    teamDao.findSeason(seasonId).map {
      case Some(season) => {
        logger.info("Found sesaon " + season)
        teamDao.list.map(teams => {
          logger.info("Loaded team dictionary")
          scrape(seasonId,updatedBy,teams,date)
        })
      }}
    Future.successful(Redirect(routes.Application.admin()))
  }

  def scrapeOneDay(seasonId:Long, year:Int, month:Int, day:Int) = scrapeLocalDate(seasonId, LocalDate.of(year,month,day))


  def scrapeSeason(seasonId: Long) = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Scraping season")
    val updatedBy: String = "Scraper[" + rs.identity.userID.toString + "]"

    teamDao.findSeason(seasonId).map {
      case Some(season) => {
        logger.info("Found sesaon "+season)
        teamDao.list.map(teams => {
          logger.info("Loaded team dictionary")

          val dates: List[LocalDate] = season.dates.toList
          dates.map(d => scrape(seasonId, updatedBy, teams, d))
        })
        Redirect(routes.Application.index())
      }
      case None => Redirect(routes.Application.admin()).flashing("error" -> "Season was not found.  Unable to scrape")
    }
  }

  def scrape(seasonId: Long, updatedBy: String, teams: List[Team], d: LocalDate): Future[List[Long]] = {
    val teamDict = teams.map(t => t.key -> t).toMap
    logger.info("Loading date " + d)
    (teamLoad ? ScoreboardByDateReq(d))
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
      logger.info("Faled to map game "+gd)
      None
    }
  }
}