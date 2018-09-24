package modules

import com.fijimf.deepfij.models.RSSFeedStatus
import jobs._
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.concurrent.AkkaGuiceSupport

/**
  * The job module.
  */
class JobModule extends ScalaModule with AkkaGuiceSupport {

  /**
    * Configures the module.
    */
  def configure() = {
 //   bindActor[AuthTokenCleaner](AuthTokenCleaner.name)
    bindActor[ScheduleUpdater](ScheduleUpdater.name)
    bindActor[StatsUpdater](StatsUpdater.name)
    bindActor[PredictionsUpdater](PredictionsUpdater.name)
    bindActor[RssUpdater](RssUpdater.name)
    bind[Scheduler].asEagerSingleton()
  }
}