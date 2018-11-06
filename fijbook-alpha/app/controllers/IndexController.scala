package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api.Silhouette
import play.api.mvc.{BaseController, ControllerComponents}
import controllers.silhouette.utils.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 val teamDao: ScheduleDAO,
                                 val userService: UserService,
                                 val silhouette: Silhouette[DefaultEnv],
                                 val s3BlockController: S3BlockController)(implicit ec: ExecutionContext)
  extends BaseController {

  def index = silhouette.UserAwareAction.async { implicit rs =>
    s3BlockController.staticBlock("index")(rs)
  }

  def redirect = silhouette.UserAwareAction.async { implicit rs =>
    Future {
      Redirect(routes.ReactMainController.index())
    }
  }

  def about() = silhouette.UserAwareAction.async { implicit rs =>
    s3BlockController.staticBlock("about")(rs)
  }
}
