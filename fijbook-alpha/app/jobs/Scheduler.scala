package jobs

import akka.actor.{ ActorRef, ActorSystem }
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
/**
  * Schedules the jobs.
  */
class Scheduler @Inject() (
                            system: ActorSystem,
                            @Named("auth-token-cleaner") authTokenCleaner: ActorRef,
                            @Named("schedule-updater") scheduleUpdater: ActorRef,
                            @Named("stats-updater") statsUpdater: ActorRef,
                            @Named("predictions-updater") predictionsUpdater: ActorRef,
                            @Named("memory-monitor") memoryMonitor: ActorRef) {
  QuartzSchedulerExtension(system).schedule("AuthTokenCleaner", authTokenCleaner, AuthTokenCleaner.Clean)
  authTokenCleaner ! AuthTokenCleaner.Clean
  QuartzSchedulerExtension(system).schedule("DailyScheduleUpdater", scheduleUpdater, ScheduleUpdater.forDailyUpdate)
  QuartzSchedulerExtension(system).schedule("IntradayScheduleUpdater", scheduleUpdater, ScheduleUpdater.forNow)
  QuartzSchedulerExtension(system).schedule("DailyStatsUpdater", statsUpdater, StatsUpdater.Update(Some(7)))
  QuartzSchedulerExtension(system).schedule("DailyPredictionsUpdater", predictionsUpdater, PredictionsUpdater.Update)
  QuartzSchedulerExtension(system).schedule("MemoryWatchdog", memoryMonitor, MemoryMonitor.CheckMemory)
}