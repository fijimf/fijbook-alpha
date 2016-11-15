package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.services.UserService
import com.fijimf.deepfij.models.{Schedule, ScheduleDAO, ScheduleRepository, User}
import com.mohiva.play.silhouette.api.Silhouette
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future

class AdminController @Inject()(val userService: UserService, val scheduleDao:ScheduleDAO, val silhouette: Silhouette[DefaultEnv]) extends Controller {

  def index = silhouette.UserAwareAction.async { implicit rs =>
    for (
      users <- userService.list.recover { case (thr: Throwable) => List.empty[User] };
      schedules <- scheduleDao.loadSchedules.recover { case (thr: Throwable) => List.empty[Schedule] }

    ) yield {
      Ok(views.html.admin.index(rs.identity, users, schedules))
    }
  }
}
