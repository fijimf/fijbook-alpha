package controllers

import com.fijimf.deepfij.models.QuoteVote
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.react.{DisplayLink, DisplayUser}
import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import org.apache.commons.lang3.StringUtils
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BaseController, ControllerComponents}
import play.twirl.api.Html

import scala.concurrent.duration._
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
  extends BaseController with WithDao with ReactEnricher {


  def index() = silhouette.UserAwareAction.async { implicit rs =>
    loadDisplayUser(rs).map(du=>Ok(views.html.reactMain(du)))
  }

  def displayUser()  = silhouette.UserAwareAction.async { implicit rs =>
    loadDisplayUser(rs).map(du => Ok(s"var displayUser=${Json.toJson(du)}").as(JAVASCRIPT))
  }
}




