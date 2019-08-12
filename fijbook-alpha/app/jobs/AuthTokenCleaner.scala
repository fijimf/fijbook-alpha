package jobs

import akka.actor._
import com.mohiva.play.silhouette.api.util.Clock
import javax.inject.Inject
import cats.implicits._
import com.fijimf.deepfij.auth.services.AuthTokenService
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global

class AuthTokenCleaner @Inject() (service: AuthTokenService, clock: Clock) extends Actor {

  val logger: Logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case s: String if s === "CleanTokens" =>
      val mysender = sender()
      val start = clock.now.getMillis
      service.clean.map { deleted =>
        val seconds = (clock.now.getMillis - start) / 1000
        "Total of %s auth tokens(s) were deleted in %s seconds".format(deleted.length, seconds)
      }.onComplete(mysender ! _)
  }
}

/**
  * The companion object.
  */
object AuthTokenCleaner {
  case object Clean
  val name = "auth-token-cleaner"
}