package controllers

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api.{LogoutEvent, Silhouette}
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}


/**
  * reactMain is a bastard of a controller, becausae I really have no ide what I'm doing.
  *
  * @param controllerComponents
  * @param dao
  * @param userService
  * @param silhouette
  * @param s3BlockController
  * @param ec
  */
class ReactMainController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 val dao: ScheduleDAO,
                                 val userService: UserService,
                                 val silhouette: Silhouette[DefaultEnv],
                                 val s3BlockController: S3BlockController)(implicit ec: ExecutionContext)
  extends BaseController with WithDao with ReactEnricher with I18nSupport {


  def index() = silhouette.UserAwareAction.async { implicit rs =>
    Future.successful(Ok(views.html.reactMain("FrontPage")))
  }

  def signIn() = silhouette.UnsecuredAction.async { implicit rs =>
    Future.successful(Ok(views.html.reactMain("SignIn")))
  }

  def signUp() = silhouette.UnsecuredAction.async { implicit rs =>
    Future.successful(Ok(views.html.reactMain("SignUp")))
  }

  def signOut() = silhouette.SecuredAction.async { implicit rs =>
    val result = Redirect("/r/index")
    silhouette.env.eventBus.publish(LogoutEvent(rs.identity, rs))
    silhouette.env.authenticatorService.discard(rs.authenticator, result)
  }

  def loadUser() = silhouette.UserAwareAction.async { implicit rs =>
    loadDisplayUser(rs).map(du => Ok(s"var displayUser=${Json.toJson(du)}").as(JAVASCRIPT))
  }
}




