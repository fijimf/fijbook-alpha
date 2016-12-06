package controllers

import com.fijimf.deepfij.models.ScheduleDAO
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future

class TeamController @Inject()(val teamDao: ScheduleDAO, silhouette: Silhouette[DefaultEnv], val messagesApi: MessagesApi) extends Controller with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)

  def team(key:String) = silhouette.UserAwareAction.async { implicit request =>

    teamDao.loadSchedules().map(ss=> {
      val sortedSchedules  = ss.sortBy(s => -s.season.year)
      sortedSchedules.headOption match {
        case Some(sch)=> {
          Ok(views.html.data.team( request.identity,sch.keyTeam(key), sch,sortedSchedules.tail))
        }
        case None=> Redirect(routes.IndexController.index()).flashing("info"->"No current schedule loaded")
      }

    })

  }
  def teams() = play.mvc.Results.TODO //silhouette.UserAwareAction.async { implicit request =>
//
//    Future.successful(play.mvc.Results.TODO)
//
//  }
}