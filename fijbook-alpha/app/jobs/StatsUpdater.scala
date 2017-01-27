package jobs

import java.time.LocalDate
import javax.inject.Inject

import akka.actor.Actor
import com.fijimf.deepfij.models.services.{ScheduleUpdateService, StatisticWriterService}
import jobs.ScheduleUpdater.Update

class StatsUpdater @Inject()(svc:StatisticWriterService) extends Actor {

  val logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case Update => svc.update(Some(7))//<-Rerun the last week
  }
}

object StatsUpdater {

  case object Update

}

