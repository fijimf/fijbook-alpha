package controllers

import java.time.LocalDate

import com.amazonaws.util.IOUtils
import com.fijimf.deepfij.models.S3StaticAsset
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.react.{DisplayLink, DisplayUser}
import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import org.apache.commons.lang3.StringUtils
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class ReactMainController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 val dao: ScheduleDAO,
                                 val userService: UserService,
                                 val silhouette: Silhouette[DefaultEnv],
                                 val s3BlockController: S3BlockController)(implicit ec: ExecutionContext)
  extends BaseController {


  def index() = silhouette.UserAwareAction.async { implicit rs =>


    val lookupName=  rs.identity.map(_.name) match {
      case None=> rs.connection.remoteAddressString
      case Some(n) if StringUtils.isBlank(n)=> rs.connection.remoteAddressString
      case Some(n)=>n
    }

    val favorites = rs.identity match {
      case Some(u) =>
        dao.findFavoriteLinksByUser(u.userID.toString).map(_.map(fl => DisplayLink(fl.displayAs, fl.link, "fa-star")))
      case None =>
        Future.successful(List.empty[DisplayLink])
    }

    val likedQuotes = dao.findQuoteVoteByUser(lookupName, 24.hours)
    for {
      f<-favorites
      l<-likedQuotes
    } yield {
      val user = DisplayUser(rs.identity, rs.identity.exists(_.isDeepFijAdmin),f,l.map(_.quoteId.toInt))
        Ok(views.html.reactMain(user))
    }

  }
}
