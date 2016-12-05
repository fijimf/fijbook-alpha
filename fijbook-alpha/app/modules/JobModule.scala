package modules

import jobs.{AuthTokenCleaner, ScheduleUpdater, Scheduler}
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
    bind[Scheduler].asEagerSingleton()
  }
}