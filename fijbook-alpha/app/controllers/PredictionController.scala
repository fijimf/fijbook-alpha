package controllers

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.GamePredictorService
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import controllers.silhouette.utils.DefaultEnv

import scala.concurrent.Future

class PredictionController @Inject()(
                                      val controllerComponents: ControllerComponents,
                                      val teamDao: ScheduleDAO,
                                      val gamePredictorService: GamePredictorService,
                                      val silhouette: Silhouette[DefaultEnv]
                                    )
  extends BaseController with I18nSupport {
  val log = Logger(this.getClass)

  def update() = silhouette.SecuredAction.async { implicit rs =>
    gamePredictorService.update()
    Future.successful(
      Redirect(routes.AdminController.index()).flashing("info" -> "Updating models for current schedule")
    )
  }

}
