package jobs

import javax.inject.Inject

import akka.actor.Actor
import com.fijimf.deepfij.models.services.ComputedStatisticService

class StatsUpdater @Inject()(svc: ComputedStatisticService) extends Actor {

  val logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case StatsUpdater.Update(whichDays) => svc.update(whichDays) //<-Rerun the last week
  }
}

object StatsUpdater {

  case class Update(whichDays: Option[Int])
  val name = "stats-updater"

}

