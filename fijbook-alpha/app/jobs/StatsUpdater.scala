package jobs

import javax.inject.Inject

import akka.actor.Actor
import com.fijimf.deepfij.models.services.ComputedStatisticService

class StatsUpdater @Inject()(svc: ComputedStatisticService) extends Actor {

  val logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case year:String =>
      logger.info(s"Received request to update $year")
      svc.update(year.toInt)
    case s:Any=>logger.warn(s"Unknown message recieved by StatsUpdater: $s")
  }
}

object StatsUpdater {
  final case class Update(yyyy:String)
  val name = "stats-updater"
}

