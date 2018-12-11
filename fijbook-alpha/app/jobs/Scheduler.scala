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
                            @Named("rss-updater") rssFeedUpdater: ActorRef
                          ) {

println(s"*********** ${authTokenCleaner.path.toStringWithoutAddress}")
  QuartzSchedulerExtension(system).schedule("AuthTokenCleaner", authTokenCleaner, AuthTokenCleaner.Clean)
  authTokenCleaner ! AuthTokenCleaner.Clean
  QuartzSchedulerExtension(system).schedule("DailyScheduleUpdater", scheduleUpdater, ScheduleUpdater.forDailyUpdate)
  QuartzSchedulerExtension(system).schedule("IntradayScheduleUpdater", scheduleUpdater, ScheduleUpdater.forNow)
  //QuartzSchedulerExtension(system).schedule("DailyStatsUpdater", statsUpdater, StatsUpdater.Update(Some(7)))
  QuartzSchedulerExtension(system).schedule("RssFeedUpdateSchedule", rssFeedUpdater, RssUpdater.Update)
  QuartzSchedulerExtension.get(system).schedules.foreach{case (name,sch)=>{
    println(s"$name ${sch.name} ${sch.schedule}")
  }}
}