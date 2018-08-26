package controllers

import java.util.UUID

import akka.actor.ActorRef
import akka.contrib.throttle.Throttler
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.{ComputedStatisticService, RssFeedUpdateServiceImpl}
import com.fijimf.deepfij.scraping.UberScraper
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import controllers.silhouette.utils.DefaultEnv

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class UberScrapeController @Inject()(
                                      val controllerComponents: ControllerComponents,
                                      @Named("data-load-actor") teamLoad: ActorRef,
                                      @Named("throttler") throttler: ActorRef,
                                      val dao: ScheduleDAO,
                                      val repo: ScheduleRepository,
                                      val schSvc:RssFeedUpdateServiceImpl,
                                      val statSvc:ComputedStatisticService,
                                      silhouette: Silhouette[DefaultEnv])
  extends BaseController with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)
  implicit val timeout: Timeout = Timeout(600.seconds)
  throttler ! Throttler.SetTarget(Some(teamLoad))

  def uberScrape() = silhouette.SecuredAction.async { implicit rs =>
    val us = UberScraper(dao, repo, schSvc, statSvc, throttler)
//    val f = us.masterRebuild(UUID.randomUUID().toString, 2014, 2018)
    val f = us.masterRebuild(UUID.randomUUID().toString, 2014, 2018)
    f.onComplete{
      case Success(trs) => logger.info("Uber Scrape succeeded:\n"+trs.map(t=>s"${t.stamp.toString}  ${t.step}"))
      case Failure(ex) => logger.error(s"Uber Scrape failed with error ${ex.getMessage}", ex)
    }
    Future.successful(Redirect(routes.AdminController.index()).flashing("info" -> "Performing uber scrape"))
  }
  def markNcaaGames(filename:String) = silhouette.SecuredAction.async { implicit rs =>
    val us = UberScraper(dao, repo, schSvc, statSvc, throttler)
//    val f = us.masterRebuild(UUID.randomUUID().toString, 2014, 2018)
    val f = us.updateForTournament("/"+filename)
    f.onComplete{
      case Success(trs) => logger.info("Uber Scrape succeeded:\n"+trs.map(t=>s"${t.stamp.toString}  ${t.step}"))
      case Failure(ex) => logger.error(s"Uber Scrape failed with error ${ex.getMessage}", ex)
    }
    Future.successful(Redirect(routes.AdminController.index()).flashing("info" -> "Performing ncaa tourney step"))
  }

}