package controllers

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import com.fijimf.deepfij.models.ScheduleRepository
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.RssFeedUpdateServiceImpl
import com.fijimf.deepfij.scraping.nextgen.ScrapingManagerActor
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Silhouette
import play.api.i18n.I18nSupport
import play.api.libs.streams.ActorFlow
import play.api.libs.ws.WSClient
import play.api.mvc.{BaseController, ControllerComponents, WebSocket}
import controllers.silhouette.utils.DefaultEnv

import scala.concurrent.Future

class ScrapingManagerController @Inject()(val controllerComponents: ControllerComponents,
                                          @Named("super-scraper") superScraper: ActorRef,
                                          val ws: WSClient,
                                          val dao: ScheduleDAO,
                                          val repo:ScheduleRepository,
                                          val schedSvc:RssFeedUpdateServiceImpl,
                                          silhouette: Silhouette[DefaultEnv]) (implicit system: ActorSystem, mat: Materializer)
  extends BaseController with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def page() = silhouette.SecuredAction.async { implicit rs =>
    Future {
      Ok(views.html.admin.manageScraping(rs.identity, rs.request.host))
    }
  }

  def socket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      ScrapingManagerActor.props(out, superScraper, ws,repo,dao, schedSvc)
    }
  }
}


