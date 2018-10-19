package jobs

import java.time.LocalDateTime

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{Job, JobRun}
import com.google.inject.Inject

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

final case class ExecuteScheduled(jobName: String, message: String)
final case class ExecuteScheduledJob(job: Job)

final case class CreateWrappedJob(job: Job)

class JobWrapperActor(dao: ScheduleDAO) extends Actor {
  val logger = play.api.Logger(this.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  override def receive: Receive = {
    case CreateWrappedJob(j) =>
      logger.info(s"Creating an actor for ${j.name}")
      j.actorClass match {
        case Some(c) =>
          val ref = context.system.actorOf(Props(Class.forName(c)), j.name)
          logger.info(s"Created ${ref.path.toStringWithoutAddress}")
        case None =>
          logger.info(s"No actor class supplied for ${j.name}, assuming actor created at path ${j.actorPath}")
      }
    case ExecuteScheduled(jobName, message) =>
      logger.info(s"Executing $jobName '$message'")
      dao.findJobByName(jobName).flatMap {
        case Some(job) =>
          runJob(job, message)
        case None =>
          logger.error(s"No such job  '$jobName' found in DB")
          dao.saveJobRun(JobRun(0L, -1, LocalDateTime.now(), None, "Failure", s"Job named $jobName was not found"))
      }
    case ExecuteScheduledJob(job) => runJob(job, job.message)
    case m:Any => logger.error(s"Unknown message $m")
  }

   def runJob(job:Job, message: String): Future[_] = {
    logger.info(s"Found job $job in DB")
    dao.saveJobRun(JobRun(0L, job.id, LocalDateTime.now(), None, "Running", "")).flatMap(run => {
      if (job.isEnabled) {
        val response = context.actorSelection(job.actorPath).ask(message)(job.timeout)
        response.onComplete {
          case Success(a: Any) =>
            logger.info("Job succeeded")
            dao.saveJobRun(run.copy(endTime = Some(LocalDateTime.now()), status = "Success", message = a.toString))
          case Failure(thr: Throwable) =>
            logger.error("Job failed", thr)
            dao.saveJobRun(run.copy(endTime = Some(LocalDateTime.now()), status = "Failure", message = thr.getMessage))
        }
        response
      } else {
        dao.saveJobRun(run.copy(endTime = Some(LocalDateTime.now()), status = "Success", message = "Job is not enabled -- skipping execution."))
      }
    })
  }
}


/**
  *
  * For a job to be scheduled to run, it needs to be in three places.
  * 1) NewJobModule.scala needs to bind the the name of the actor with its implementing class
  * 2) application.conf needs to attach a schedule to the name of the actor
  * 3) The job table in the database needs to know the name of the job
  *
  * @param system
  * @param dao
  *
  *
  */
class NewScheduler @Inject()(system: ActorSystem, dao: ScheduleDAO) {

  import scala.concurrent.ExecutionContext.Implicits._

  val logger = play.api.Logger(this.getClass)

  private val wrapper: ActorRef = system.actorOf(Props(classOf[JobWrapperActor], dao), "wrapper")

  logger.info(dao.toString)
  logger.info(Await.result(dao.listJobs, Duration.Inf).mkString(","))
  dao.listJobs.map(jobs => {
    logger.info(s"Found ${jobs.size} to initialize")
    jobs.foreach(j => {
      logger.info(s"Creating job actor ${j.name}")
      wrapper ! CreateWrappedJob(j)
    })
    jobs.foreach(j => {
      logger.info(s"Scheduling job ${j.name}")
      DeepFijQuartzSchedulerExtension(system).scheduleJob(j, wrapper)
    })
  })
}