package controllers

import javax.inject.Inject
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.{ScheduleSerializer, UserService}
import com.fijimf.deepfij.models.{Schedule, ScheduleRepository, User}
import com.mohiva.play.silhouette.api.Silhouette
import play.api.mvc.{BaseController, ControllerComponents}
import utils.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

class AdminController @Inject()(val controllerComponents: ControllerComponents, val userService: UserService, val scheduleDao: ScheduleDAO, val repo:ScheduleRepository, silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext) extends BaseController {

  def index = silhouette.UserAwareAction.async { implicit rs =>
    for (
      users <- userService.list.recover { case (thr: Throwable) => List.empty[User] };
      schedules <- scheduleDao.loadSchedules.recover { case (thr: Throwable) => List.empty[Schedule] }

    ) yield {
      Ok(views.html.admin.index(rs.identity, users, schedules))
    }
  }
  
  def writeSnapshot = silhouette.SecuredAction.async  { implicit rs =>
    ScheduleSerializer.writeSchedulesToS3(scheduleDao).map(_=>Redirect(routes.AdminController.listSnapshots()))
  }
  def readSnapshot(key:String) = silhouette.SecuredAction.async  { implicit rs =>
    ScheduleSerializer.readSchedulesFromS3(key,scheduleDao,repo).map(_=>Redirect(routes.AdminController.index()))
  }

  def listSnapshots = silhouette.SecuredAction.async  { implicit rs =>
    Future {
      Ok(views.html.admin.browseSnapshots(rs.identity,ScheduleSerializer.listSaved()))
    }
  }

  def userProfile(id: String) = play.mvc.Results.TODO
}
