package com.fijimf.deepfij.scraping.nextgen

import java.text.DecimalFormat
import java.time.{Duration => JDuration}

import akka.actor.{Actor, ActorRef, Props}
import akka.contrib.throttle.Throttler.Rate
import akka.contrib.throttle.{Throttler, TimerBasedThrottler}
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.schedule.services.ScheduleUpdateService
import com.fijimf.deepfij.scraping.nextgen.ScrapeManagingActor.ScrapeThrottler
import com.fijimf.deepfij.scraping.{FormatDuration, ScrapingActor}
import com.fijimf.deepfij.scraping.nextgen.tasks._
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.duration._

object ScrapingManagerActor {
  def props(out: ActorRef,
            superScraper: ActorRef,
            ws: WSClient,
            repo: ScheduleRepository,
            dao: ScheduleDAO,
            schedSvc:ScheduleUpdateService
           ) = Props(new ScrapingManagerActor(out, superScraper, ws, repo, dao, schedSvc))
}

final case class ScrapingManagerActor(
                                 out: ActorRef,
                                 superScraper: ActorRef,
                                 ws: WSClient,
                                 repo: ScheduleRepository,
                                 dao: ScheduleDAO,
                                 schedSvc:ScheduleUpdateService
                               ) extends Actor {

  val teamThrottler: ActorRef = throttledScrapingClient(Rate(3,1.second))
  val confThrottler: ActorRef = throttledScrapingClient(Rate(4,1.second))
  val gameThrottler: ActorRef = throttledScrapingClient(Rate(5,1.second))

  val log = Logger(this.getClass)

  override def receive: Receive = {
    case (s: String) if s.toLowerCase == "$command::status" =>
      log.info("Received status command")
      superScraper ! SSStatus
    case (s: String) if s.toLowerCase == "$command::cancel_task_list" =>
      log.info("Received cancel task list command")
      superScraper ! SSCancelTaskList
    case (s: String) if s.toLowerCase == "$command::cancel_task" =>
      log.info("Received cancel task command")
      superScraper ! SSCancelTask
    case (s: String) if s.toLowerCase == "$command::status" =>
      log.info("Received status command")
      superScraper ! SSStatus
    case (s: String) if s.toLowerCase == "$command::full_rebuild" =>
      log.info("Received full rebuild command")
      val msg = SSProcessTasks(List(
        RebuildDatabase(repo),
        LoadAliases(dao),
        ScrapeTeams(dao, teamThrottler),
        ScrapeConferences(System.currentTimeMillis().toString, dao, confThrottler),
        CreateSeasons(2014,2018, dao),
        SeedConferenceMaps(dao),
        ScrapeGames(dao, gameThrottler),
        TourneyUpdateTask("/ncaa-tourn.txt",dao),
        NeutralSiteSolver(dao)
      ))
      superScraper ! msg
    case (r: ReadyData) =>
      log.info("Received ~READY~")
      out ! Json.toJson(r).toString()
    case (p: ProcessingData) =>
      log.info("Received ~PROCESSING-DATA~")
      out ! Json.toJson(p).toString()
    case (taskId: String, elapsedTime:JDuration,pctComplete:Double, progress: String) =>
      log.info("Recieved ~PROGRESS~")
      out ! Json.toJson(Map(
        "type" -> "progress",
        "taskId" -> taskId,
        "elapsedTime"->FormatDuration(elapsedTime),
        "progress" -> progress,
        "percentComplete"->(new DecimalFormat("#.00").format(100*pctComplete)+"%"))).toString()
    case _ =>
      log.error("Received an unexpected message")
  }

  private def throttledScrapingClient(r:Rate):ActorRef= {
    val dataActor: ActorRef = context.actorOf(ScrapingActor.props(ws))
    val throttler: ActorRef = context.actorOf(ScrapeThrottler.props(Rate(2, 1.second)))
    throttler ! Throttler.SetTarget(Some(dataActor))
    throttler
  }
}


object ScrapeManagingActor {

  object ScrapeThrottler {
    def props(r: Rate) = Props(classOf[TimerBasedThrottler], r)
  }

}