package jobs

import javax.inject.Inject
import akka.actor.Actor
import com.fijimf.deepfij.statistics.services.ComputedStatisticService

import scala.concurrent.duration._
import scala.util.{Failure, Success}
class StatsUpdater @Inject()(svc: ComputedStatisticService) extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case year:String =>
      logger.info(s"Received request to update all statistics for season $year")
      val capture = sender()
      svc.update(year.toInt, 1.hour).onComplete{
        case Success(msg)=>
          logger.info(s"Update complete sending status $msg to original caller ${capture.path.toStringWithoutAddress}")
          capture ! msg
        case Failure(thr)=>
          logger.error(s"Update failed sending status ${thr.getMessage} to original caller ${capture.path.toStringWithoutAddress}")
          capture ! thr.getMessage
      }
    case s:Any=>logger.warn(s"Unknown message received by StatsUpdater: $s")
  }
}

object StatsUpdater {
  val name = "stats-updater"
}

