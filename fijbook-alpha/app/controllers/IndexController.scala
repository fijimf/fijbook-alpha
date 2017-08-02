package controllers

import java.time.LocalDate
import javax.inject.Inject

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.fijimf.deepfij.models.{Game, ScheduleRepository, User}
import com.mohiva.play.silhouette.api.Silhouette
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BaseController, Controller, ControllerComponents}
import utils.DefaultEnv

import scala.concurrent.Future

class IndexController @Inject()(
                                 val controllerComponents:ControllerComponents,
                                 val teamDao: ScheduleDAO,
                                 val userService: UserService,
                                 val silhouette: Silhouette[DefaultEnv],
                                 val s3BlockController:S3BlockController )
  extends BaseController {

  def index = silhouette.UserAwareAction.async { implicit rs =>
   s3BlockController.staticBlock("index")(rs)
  }

  def redirect = silhouette.UserAwareAction.async { implicit rs =>
    Future {
      Redirect(routes.IndexController.index())
    }
  }

  def about() =  silhouette.UserAwareAction.async { implicit rs =>
    s3BlockController.staticBlock("about")(rs)
  }
}
