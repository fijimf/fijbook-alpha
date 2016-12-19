package controllers

import com.fijimf.deepfij.models.ScheduleDAO
import com.fijimf.deepfij.models.services.StatisticWriterService
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Controller}
import utils.DefaultEnv

import scala.util.{Failure, Success, Try}

class StatsController @Inject()(val teamDao: ScheduleDAO, val statWriterService: StatisticWriterService, val silhouette: Silhouette[DefaultEnv], val messagesApi: MessagesApi) extends Controller with I18nSupport {
val log=Logger(this.getClass)
  import scala.concurrent.ExecutionContext.Implicits.global
  def updateAll(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>

    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      sortedSchedules.headOption match {
        case Some(sch) => {
          statWriterService.updateForSchedule(sch).onComplete {
            case Success(x) =>
              log.info("************-->>" + x + "<<--*************")
            case Failure(thr) =>
              log.error("", thr)
              Redirect(routes.AdminController.index()).flashing("error" -> "Exception while updating models")
          }
          Redirect(routes.AdminController.index()).flashing("info" -> "Updating models for current schedule")
        }
        case None => Redirect(routes.IndexController.index()).flashing("info" -> "No current schedule loaded")
      }

    })

  }

}
