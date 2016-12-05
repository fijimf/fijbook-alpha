package jobs

import akka.actor.{ ActorRef, ActorSystem }
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension

/**
  * Schedules the jobs.
  */
class Scheduler @Inject() (system: ActorSystem, @Named("auth-token-cleaner") authTokenCleaner: ActorRef, @Named("schedule-updater") scheduleUpdater: ActorRef) {
  QuartzSchedulerExtension(system).schedule("AuthTokenCleaner", authTokenCleaner, AuthTokenCleaner.Clean)
  authTokenCleaner ! AuthTokenCleaner.Clean
  QuartzSchedulerExtension(system).schedule("ScheduleUpdater", scheduleUpdater, ScheduleUpdater.Update)
  scheduleUpdater ! ScheduleUpdater.Update
}