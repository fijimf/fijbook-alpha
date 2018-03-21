package controllers

import javax.inject.Inject
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.{ScheduleSerializer, UserService}
import com.fijimf.deepfij.models.{Schedule, User}
import com.mohiva.play.silhouette.api.Silhouette
import play.api.mvc.{BaseController, ControllerComponents}
import utils.DefaultEnv

import scala.concurrent.ExecutionContext

class AdminController @Inject()(val controllerComponents: ControllerComponents, val userService: UserService, val scheduleDao: ScheduleDAO, val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext) extends BaseController {

  def index = silhouette.UserAwareAction.async { implicit rs =>
    for (
      users <- userService.list.recover { case (thr: Throwable) => List.empty[User] };
      schedules <- scheduleDao.loadSchedules.recover { case (thr: Throwable) => List.empty[Schedule] }

    ) yield {
      Ok(views.html.admin.index(rs.identity, users, schedules))
    }
  }
  
  def serializeCurrent = silhouette.UserAwareAction.async { implicit rs =>
    ScheduleSerializer.writeSchedulesToS3(scheduleDao).map(Ok(_))
  }

  def userProfile(id: String) = play.mvc.Results.TODO
}
