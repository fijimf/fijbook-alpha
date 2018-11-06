package jobs

import java.util.{Date, TimeZone}

import akka.actor.{ActorRef, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.event.{LogSource, Logging}
import com.fijimf.deepfij.models.Job
import com.typesafe.akka.extension.quartz.{QuartzCalendars, SimpleActorMessageJob}
import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import org.quartz
import org.quartz._
import org.quartz.core.jmx.JobDataMapSupport
import org.quartz.impl.DirectSchedulerFactory
import org.quartz.simpl.{RAMJobStore, SimpleThreadPool}
import org.quartz.spi.{JobStore, ThreadPool}

import scala.concurrent.Future
import scala.util.Try

object DeepFijQuartzSchedulerExtension extends ExtensionId[DeepFijQuartzSchedulerExtension] with ExtensionIdProvider {
  override def lookup: DeepFijQuartzSchedulerExtension.type = DeepFijQuartzSchedulerExtension

  override def createExtension(system: ExtendedActorSystem): DeepFijQuartzSchedulerExtension = new DeepFijQuartzSchedulerExtension(system)
}

class DeepFijQuartzSchedulerExtension(system: ExtendedActorSystem) extends Extension {
  implicit val myLogSourceType: LogSource[DeepFijQuartzSchedulerExtension] = new LogSource[DeepFijQuartzSchedulerExtension] {
    def genString(a: DeepFijQuartzSchedulerExtension): String = a.schedulerName
  }

  private val log = Logging(system, this)

  val schedulerName: String = "DeepFijScheduler~%s".format(system.name)

  protected val config: Config = system.settings.config.withFallback(defaultConfig).getConfig("akka.quartz").root.toConfig

  log.info(config.root().render(ConfigRenderOptions.defaults().setComments(false).setOriginComments(false)))

  // For config values that can be omitted by user, to setup a fallback
  lazy val defaultConfig: Config = ConfigFactory.parseString(
    """
    akka.quartz {
      threadPool {
        threadCount = 1
        threadPriority = 5
        daemonThreads = true
      }
      defaultTimezone = UTC
    }""".stripMargin) // todo - boundary checks

  val threadCount: Int = config.getInt("threadPool.threadCount")
  val threadPriority: Int = config.getInt("threadPool.threadPriority")
  require(threadPriority >= 1 && threadPriority <= 10,
    "Quartz Thread Priority (akka.quartz.threadPool.threadPriority) must be a positive integer between 1 (lowest) and 10 (highest).")
  val daemonThreads: Boolean = config.getBoolean("threadPool.daemonThreads")
  val defaultTimezone: TimeZone = TimeZone.getTimeZone(config.getString("defaultTimezone"))

  /**
    * Parses job and trigger configurations, preparing them for any code request of a matching job.
    * In our world, jobs and triggers are essentially 'merged'  - our scheduler is built around triggers
    * and jobs are basically 'idiot' programs who fire off messages.
    *
    * RECAST KEY AS UPPERCASE TO AVOID RUNTIME LOOKUP ISSUES
    */

  //  val runningJobs: mutable.Map[String, JobKey] = mutable.Map.empty[String, JobKey]


  initialiseCalendars()

  /**
    * Puts the Scheduler in 'standby' mode, temporarily halting firing of triggers.
    * Resumable by running 'start'
    */
  def standby(): Unit = scheduler.standby()

  def isInStandbyMode: Boolean = scheduler.isInStandbyMode

  /**
    * Starts up the scheduler. This is typically used from userspace only to restart
    * a scheduler in standby mode.
    *
    * @return True if calling this function resulted in the starting of the scheduler; false if the scheduler
    *         was already started.
    */
  def start(): Boolean = if (isStarted) {
    log.warning("Cannot start scheduler, already started.")
    false
  } else {
    scheduler.start()
    true
  }

  def isStarted: Boolean = scheduler.isStarted

  /**
    * Returns the next Date a schedule will be fired
    */
  def nextTrigger(j: Job): Option[Date] = {
    Try {
      Option(scheduler.getTrigger(j.quartzTriggerKey))
    }.toOption.flatten.map(_.getNextFireTime)
  }

  /**
    * Suspends (pauses) all jobs in the scheduler
    */
  def suspendAll(): Unit = {
    log.info("Suspending all Quartz jobs.")
    scheduler.pauseAll()
  }

  /**
    * Shutdown the scheduler manually. The scheduler cannot be re-started.
    *
    * @param waitForJobsToComplete wait for jobs to complete? default to false
    */
  def shutdown(waitForJobsToComplete: Boolean = false): Unit = {
    scheduler.shutdown(waitForJobsToComplete)
  }

  /**
    * Cancels the running job and all associated triggers
    *
    * @param name The name of the job, as defined in the schedule
    * @return Success or Failure in a Boolean
    */
  def cancelJob(j: Job): Boolean = {
    log.info("Cancelling Quartz Job '{}'", j.quartzJobKey)
    scheduler.deleteJob(j.quartzJobKey)
  }

  //  protected def scheduleJob(name: String, receiver: AnyRef, msg: AnyRef, startDate: Option[Date])(schedule: QuartzSchedule): Date = {
  def scheduleJob(j: Job): Future[Date] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration._
    system.actorSelection("/user/wrapper").resolveOne(1.minute).map(scheduleJob(j, _))
  }

  def scheduleJob(j: Job, wrapper: ActorRef): Date = {

    import scala.collection.JavaConverters._
    log.info(s"Setting up scheduled job '${j.name}', with '${j.cronSchedule}'")
    val path = j.actorClass match {
      case Some(c)=> s"/user/wrapper/$j.name"
      case None => s"/user/$j.name"
    }
    val jobDataMap = Map[String, AnyRef](
      "logBus" -> system.eventStream,
      "receiver" -> wrapper,
      "message" -> ExecuteScheduled(j.name, j.message)
    )

    val jobData = JobDataMapSupport.newJobDataMap(jobDataMap.asJava)
    val job: JobDetail = JobBuilder.newJob(classOf[SimpleActorMessageJob])
      .withIdentity(j.quartzJobKey)
      .usingJobData(jobData)
      .withDescription(j.description)
      .storeDurably(true)
      .build()

    log.info(s"Adding jobKey ${job.getKey} to runningJobs map.")
    scheduler.addJob(job, true)

    log.info(s"Building Trigger with startDate '${new Date()}")

    val trigger = TriggerBuilder.newTrigger().withIdentity(j.quartzTriggerKey).forJob(job).withSchedule(CronScheduleBuilder.cronSchedule(j.cronSchedule)).startNow().build()
    if (scheduler.checkExists(j.quartzTriggerKey)) {
      log.info(s"Rescheduling Job '$job' and Trigger '$trigger'. Is Scheduler Running? ${scheduler.isStarted}")
      scheduler.rescheduleJob(j.quartzTriggerKey, trigger)
    } else {
      log.info(s"Scheduling Job '$job' and Trigger '$trigger'. Is Scheduler Running? ${scheduler.isStarted}")
      scheduler.scheduleJob(trigger)
    }
  }


  /**
    * Parses calendar configurations, creates Calendar instances and attaches them to the scheduler
    */
  protected def initialiseCalendars() {
    for ((name, calendar) <- QuartzCalendars(config, defaultTimezone)) {
      log.info("Configuring Calendar '{}'", name)
      // Recast calendar name as upper case to make later lookups easier ( no stupid case clashing at runtime )
      scheduler.addCalendar(name.toUpperCase, calendar, true, true)
    }
  }


  protected val threadPool: ThreadPool = {
    val tp = new SimpleThreadPool(threadCount, threadPriority)
    tp.setThreadNamePrefix("AKKA_QRTZ_")
    tp.setMakeThreadsDaemons(daemonThreads)
    tp
  }

  protected val jobStore: JobStore = new RAMJobStore()
  protected val scheduler: quartz.Scheduler = {
    if (DirectSchedulerFactory.getInstance().getScheduler(schedulerName)==null) {
      DirectSchedulerFactory.getInstance.createScheduler(schedulerName, system.name, threadPool, jobStore)
    }
    val scheduler = DirectSchedulerFactory.getInstance().getScheduler(schedulerName)

    log.info(s"Initialized a Quartz Scheduler '$scheduler'")

    system.registerOnTermination({
      log.info("Shutting down Quartz Scheduler with ActorSystem Termination (Any jobs awaiting completion will end as well, as actors are ending)...")
      scheduler.shutdown(false)
    })
    scheduler.start()
    scheduler
  }

}
