package jobs

import akka.actor.Actor
import com.fijimf.deepfij.news.services.RssFeedUpdateService
import javax.inject.Inject
import play.api.Logger

class RssUpdater @Inject()(svc: RssFeedUpdateService) extends Actor {

  val logger: Logger = play.api.Logger(this.getClass)

  def receive: Receive = {
    case RssUpdater.Update => svc.updateAllFeeds()
  }
}

object RssUpdater {

  case object Update

  val name = "rss-updater"
}