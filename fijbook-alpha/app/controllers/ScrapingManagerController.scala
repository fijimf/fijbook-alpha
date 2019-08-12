package controllers

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import com.fijimf.deepfij.models.ScheduleRepository
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.schedule.services.ScheduleUpdateService
import com.fijimf.deepfij.scraping.nextgen.ScrapingManagerActor
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import play.api.i18n.I18nSupport
import play.api.libs.streams.ActorFlow
import play.api.libs.ws.WSClient
import play.api.mvc._

class ScrapingManagerController @Inject()(val controllerComponents: ControllerComponents,
                                          @Named("super-scraper") superScraper: ActorRef,
                                          val ws: WSClient,
                                          val dao: ScheduleDAO,
                                          val repo:ScheduleRepository,
                                          val schedSvc:ScheduleUpdateService,
                                          silhouette: Silhouette[DefaultEnv]) (implicit system: ActorSystem, mat: Materializer)
  extends BaseController with WithDao with UserEnricher with QuoteEnricher  with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def page(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    for {du <- loadDisplayUser(rs)
         qw <- getQuoteWrapper(du)
    }yield{
      Ok(views.html.admin.manageScraping(du,qw, rs.request.host))
    }
  }

  def socket: WebSocket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      ScrapingManagerActor.props(out, superScraper, ws,repo,dao, schedSvc)
    }
  }
}


