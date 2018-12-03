package controllers

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.{ScheduleSerializer, UserService}
import com.fijimf.deepfij.models.{Schedule, ScheduleRepository, User}
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import play.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

class AdminController @Inject()
(
  val controllerComponents: ControllerComponents,
  val userService: UserService,
  val dao: ScheduleDAO,
  val repo: ScheduleRepository,
  silhouette: Silhouette[DefaultEnv]
)(implicit ec: ExecutionContext) extends BaseController with WithDao with UserEnricher with QuoteEnricher {
  val log=Logger(this.getClass)

  def index: Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
    for {
      users <- userService.list.recover { case thr: Throwable => List.empty[User] };
      schedules <- dao.loadSchedules().recover { case thr: Throwable => List.empty[Schedule] }
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      Ok(views.html.admin.index(du, qw, users, schedules))
    }
  }

  def writeSnapshot: Action[AnyContent] = silhouette.SecuredAction.async  { implicit rs =>
      ScheduleSerializer.writeSchedulesToS3(dao).map(_=>Redirect(routes.AdminController.listSnapshots()))
  }
  def readSnapshot(key:String): Action[AnyContent] = silhouette.SecuredAction.async  { implicit rs =>
   ScheduleSerializer.readSchedulesFromS3(key, dao, repo).map{case(ts,cs,gs)=>Redirect(routes.AdminController.index()).flashing("info"->s"Loaded schedule from snapshot: $ts teams, $cs conferences, $gs games")}
  }
  def deleteSnapshot(key:String): Action[AnyContent] = silhouette.SecuredAction.async  { implicit rs =>
      ScheduleSerializer.deleteSchedulesFromS3(key).map(_=>Redirect(routes.AdminController.listSnapshots()))
  }

  def listSnapshots: Action[AnyContent] = silhouette.SecuredAction.async  { implicit rs =>
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {

      Ok(views.html.admin.browseSnapshots(du,qw,ScheduleSerializer.listSaved()))
    }
  }

  def userProfile(id: String): Result = play.mvc.Results.TODO
}
