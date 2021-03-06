package modules

import com.fijimf.deepfij.news.model.RSSFeedStatus
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
  override def configure() = {
 //   bindActor[AuthTokenCleaner](AuthTokenCleaner.name)
    bindActor[ScheduleUpdater](ScheduleUpdater.name)
    bindActor[StatsUpdater](StatsUpdater.name)
    bindActor[PredictionUpdater](PredictionUpdater.name)
    bindActor[RssUpdater](RssUpdater.name)
    bind[Scheduler].asEagerSingleton()
  }
}