package controllers

import com.fijimf.deepfij.auth.services.UserService
import javax.inject.Inject
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.mohiva.play.silhouette.api.Silhouette
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import controllers.silhouette.utils.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 val teamDao: ScheduleDAO,
                                 val userService: UserService,
                                 val silhouette: Silhouette[DefaultEnv],
                                 val s3BlockController: S3BlockController)(implicit ec: ExecutionContext)
  extends BaseController {
  def redirect: Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
    Future {
      Redirect(routes.ReactMainController.index())
    }
  }
}
