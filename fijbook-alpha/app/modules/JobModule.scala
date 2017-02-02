package modules

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
    bindActor[AuthTokenCleaner]("auth-token-cleaner")
    bindActor[ScheduleUpdater]("schedule-updater")
    bindActor[StatsUpdater]("stats-updater")
    bindActor[PredictionsUpdater]("predictions-updater")
    bindActor[MemoryMonitor]("memory-monitor")
    bind[Scheduler].asEagerSingleton()
  }
}