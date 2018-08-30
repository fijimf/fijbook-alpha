package controllers

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import com.fijimf.deepfij.models.ScheduleRepository
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.stats.spark.{ClusterManager, SparkStatsManagerActor}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Silhouette
import play.api.i18n.I18nSupport
import play.api.libs.streams.ActorFlow
import play.api.libs.ws.WSClient
import play.api.mvc.{BaseController, ControllerComponents, WebSocket}
import controllers.silhouette.utils.DefaultEnv

import scala.concurrent.Future

class SparkStatsManagerController @Inject()(val controllerComponents: ControllerComponents,
                                            @Named("spark-stats") sparkStats: ActorRef,
                                            val ws: WSClient,
                                            val dao: ScheduleDAO,
                                            val repo: ScheduleRepository,
                                            silhouette: Silhouette[DefaultEnv])(implicit system: ActorSystem, mat: Materializer)
  extends BaseController with WithDao with UserEnricher with QuoteEnricher  with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def page() = silhouette.SecuredAction.async { implicit rs =>
    for {du <- loadDisplayUser(rs)
         qw <- getQuoteWrapper(du)
    }yield{
      Ok(views.html.admin.manageSparkStats(du,qw, rs.request.host))
    }
  }

  def socket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      SparkStatsManagerActor.props(out, sparkStats, ws, repo, dao)
    }
  }
}


