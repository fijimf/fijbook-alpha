package controllers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.ScheduleUpdateService
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import play.api.Logger
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class GameScrapeController @Inject()(
                                      val controllerComponents: ControllerComponents,
                                      val dao: ScheduleDAO,
                                      val scheduleUpdateService: ScheduleUpdateService,
                                      val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext)
  extends BaseController with WithDao with UserEnricher with QuoteEnricher {

  val logger = Logger(getClass)

  def scrapeGames(seasonId: Long) = silhouette.SecuredAction.async { implicit rs => {
    dao.findSeasonById(seasonId).map {
      case Some(seas) => scheduleUpdateService.updateSeason(None, seas, mailReport = false)
        Redirect(routes.AdminController.index()).flashing("info" -> ("Scraping season " + seasonId))
      case None => Redirect(routes.AdminController.index()).flashing("info" -> ("Scraping season " + seasonId))
    }
  }
  }

  def scrapeToday() = silhouette.SecuredAction.async { implicit rs => {
    scheduleUpdateService.updateSeason(Some(List(LocalDate.now())), mailReport = false)
    Future.successful(Redirect(routes.AdminController.index()).flashing("info" -> "Scraping today "))
  }
  }

  def scrapeForDay(yyyymmdd: String) = silhouette.SecuredAction.async { implicit rs => {
    val d = LocalDate.parse(yyyymmdd, DateTimeFormatter.BASIC_ISO_DATE)
    scheduleUpdateService.updateSeason(Some(List(d)), mailReport = false)
    Future.successful(Redirect(routes.AdminController.index()).flashing("info" -> s"Scraping $yyyymmdd "))
  }
  }

  def verifyResults(y:Int) = silhouette.SecuredAction.async { implicit rs =>
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      n <- scheduleUpdateService.verifyRecords(y)
    } yield {
      Ok(views.html.admin.verifyResults(du, qw, n))
    }
  }


}
