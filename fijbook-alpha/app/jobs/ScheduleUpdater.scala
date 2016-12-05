package jobs

import javax.inject.Inject

import akka.actor.Actor
import com.fijimf.deepfij.models.services.ScheduleUpdateService
import jobs.ScheduleUpdater.Update

class ScheduleUpdater @Inject()(svc:ScheduleUpdateService) extends Actor {

  val logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case Update => svc.update()

  }
}

object ScheduleUpdater {

  case object Update

}

