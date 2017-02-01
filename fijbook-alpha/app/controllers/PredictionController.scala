package controllers

import com.fijimf.deepfij.models.ScheduleDAO
import com.fijimf.deepfij.models.services.GamePredictorService
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Controller}
import utils.DefaultEnv

import scala.concurrent.Future

class PredictionController @Inject()(val teamDao: ScheduleDAO, val gamePredictorService: GamePredictorService, val silhouette: Silhouette[DefaultEnv], val messagesApi: MessagesApi) extends Controller with I18nSupport {
  val log = Logger(this.getClass)

  def update(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    gamePredictorService.update()
    Future.successful(
      Redirect(routes.AdminController.index()).flashing("info" -> "Updating models for current schedule")
    )
  }

}
