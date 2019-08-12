package controllers

import com.fijimf.deepfij.auth.services.UserService
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.ExecutionContext

class GamesController @Inject()(
                                 val controllerComponents:ControllerComponents,
                                 val teamDao: ScheduleDAO,
                                 val userService: UserService,
                                 val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext)
  extends BaseController {

val predictors = List(
  "naive-regression"->"Linear Regressor",
  "logistic-wp"->"Logistic Regressor (win %)",
  "logistic-x-margin-ties"->"Logistic Regressor (linear estimator)",
  "logistic-rpi121"->"Logistic Regressor (rpi 121)"
)


}
