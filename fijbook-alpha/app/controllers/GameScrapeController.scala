package controllers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.schedule.services.{ScheduleUpdateService, WholeSeasonUpdate}
import com.fijimf.deepfij.scraping.nextgen.tasks.TourneyUpdater
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import play.api.Logger
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class GameScrapeController @Inject()(
                                      val controllerComponents: ControllerComponents,
                                      val dao: ScheduleDAO,
                                      val scheduleUpdateService: ScheduleUpdateService,
                                      val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext)
  extends BaseController with WithDao with UserEnricher with QuoteEnricher {

  val logger = Logger(getClass)

  def scrapeGames(seasonId: Long): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs => {
    dao.findSeasonById(seasonId).map {
      case Some(seas) => scheduleUpdateService.update(WholeSeasonUpdate(seas.year))
        Redirect(routes.AdminController.index()).flashing("info" -> ("Scraping season " + seasonId))
      case None => Redirect(routes.AdminController.index()).flashing("info" -> ("Scraping season " + seasonId))
    }
  }
  }

  def scrapeToday(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs => {
    scheduleUpdateService.update(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE))
    Future.successful(Redirect(routes.AdminController.index()).flashing("info" -> "Scraping today "))
  }
  }

  def scrapeForDay(yyyymmdd: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs => {

    scheduleUpdateService.update(yyyymmdd)
    Future.successful(Redirect(routes.AdminController.index()).flashing("info" -> s"Scraping $yyyymmdd "))
  }
  }

  def verifyResults(y:Int): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      n <- scheduleUpdateService.verifyRecords(y)
    } yield {
      Ok(views.html.admin.verifyResults(du, qw, n))
    }
  }


  def updateTourneys(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs => {
    val updates = for {
      tg <- TourneyUpdater.updateNcaaTournamentGames("/ncaa-tourn.txt", dao)
      cg <- TourneyUpdater.updateConferenceTournamentGames("/conf-tourney-dates.txt", dao)
    } yield {
      tg.size + cg.size
    }
    updates.map(n => Redirect(routes.AdminController.index()).flashing("info" -> s"Updated $n games"))
  }}
}
