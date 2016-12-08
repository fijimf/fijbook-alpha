package controllers

import akka.stream.Materializer
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.mvc.{Filter, RequestHeader, Result, Results}
import utils.DefaultEnv

import scala.concurrent.Future

class AdminFilter @Inject() (silhouette: Silhouette[DefaultEnv], implicit val mat: Materializer) extends Filter {
  val admin: String = System.getProperty("admin.user","nope@nope.com")
  override def apply(next: RequestHeader => Future[Result])(
    request: RequestHeader): Future[Result] = {

    val action = silhouette.UserAwareAction.async { r =>
      if (request.path.startsWith("/deepfij/admin")) {
        r.identity.flatMap(_.email) match {
          case Some(e) if e == admin =>next(request)
          case _=> Future.successful( Results.Unauthorized)
        }
      } else {
        next(request)
      }
    }

    action(request).run
  }
}
