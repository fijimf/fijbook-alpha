package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.services.UserService
import com.fijimf.deepfij.models.{ScheduleRepository, User}
import com.mohiva.play.silhouette.api.Silhouette
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future

class Application @Inject()(val repo: ScheduleRepository, val userService: UserService, val silhouette: Silhouette[DefaultEnv])
  extends Controller {


  def index = silhouette.UserAwareAction.async { implicit rs =>
    Future {
      Ok(views.html.index(rs.identity))
    }
  }

  def admin = silhouette.UserAwareAction.async { implicit rs =>
    for (users <- userService.list.recover { case (thr: Throwable) => List.empty[User] }) yield {
      Ok(views.html.admin.index(rs.identity, users))
    }
  }

}
