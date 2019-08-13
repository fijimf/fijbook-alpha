package jobs

import akka.actor.Actor
import com.fijimf.deepfij.predictions.services.GamePredictionService
import javax.inject.Inject

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class PredictionUpdater @Inject()(svc: GamePredictionService) extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case yearKey:String =>
      val parts = yearKey.split('.')
      val year = parts(0).toInt
      val key =parts(1)
      logger.info(s"Received request to update all statistics for season $year")
      val capture = sender()
      svc.update(year,key, 1.hour).onComplete{
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

object PredictionUpdater {
  val name = "prediction-updater"
}




