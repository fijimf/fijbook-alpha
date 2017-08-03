package jobs


import javax.inject.Inject

import akka.actor.Actor
import com.fijimf.deepfij.models.services.{GamePredictorService, ScheduleUpdateService, StatisticWriterService}

class PredictionsUpdater @Inject()(svc:GamePredictorService) extends Actor {

  val logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case PredictionsUpdater.Update => svc.update()
  }
}

  object PredictionsUpdater {

    case object Update




}
