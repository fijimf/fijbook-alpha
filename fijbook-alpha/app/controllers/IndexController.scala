package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.services.UserService
import com.fijimf.deepfij.models.{ScheduleRepository, User}
import com.mohiva.play.silhouette.api.Silhouette
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future

class IndexController @Inject()(val repo: ScheduleRepository, val userService: UserService, val silhouette: Silhouette[DefaultEnv])
  extends Controller {


  def index = silhouette.UserAwareAction.async { implicit rs =>
    Future {
      Ok(views.html.index(rs.identity))
    }
  }

  def redirect = silhouette.UserAwareAction.async { implicit rs =>
    Future {
      Redirect(routes.IndexController.index())
    }
  }

}
