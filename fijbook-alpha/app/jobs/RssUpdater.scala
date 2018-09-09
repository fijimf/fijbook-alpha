package jobs

import akka.actor.Actor
import com.fijimf.deepfij.models.services.{ComputedStatisticService, RssFeedUpdateService}
import javax.inject.Inject

class RssUpdater @Inject()(svc: RssFeedUpdateService) extends Actor {

  val logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case RssUpdater.Update => svc.updateAllFeeds()
  }
}

object RssUpdater {

  case object Update

}