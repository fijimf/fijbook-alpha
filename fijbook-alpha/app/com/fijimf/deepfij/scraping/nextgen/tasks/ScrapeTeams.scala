package com.fijimf.deepfij.scraping.nextgen.tasks

import akka.actor.{ActorRef, PoisonPill}
import akka.agent.Agent
import akka.contrib.throttle.Throttler
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models.Team
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.scraping.nextgen._
import com.fijimf.deepfij.scraping.{ScrapingResponse, TeamDetail}
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success}

final case class ScrapeTeams(dao: ScheduleDAO, throttler: ActorRef) extends SSTask[Int] {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val logger = Logger(this.getClass)

  val name: String = "ScrapeTeams"

  def safeToRun: Future[Boolean] = dao.listTeams.map(_.nonEmpty)

  override def cancel = {
    throttler ! Throttler.SetTarget(None)
    throttler ! PoisonPill

  }

  def run(messageListener: Option[ActorRef]): Future[Int] = {

    val teamMaster: Future[List[Team]] = for {
      aliasMap <- loadAliasMap
      teamShortNames <- loadTeamShortNames
      ts <- scrapeKeyList(teamShortNames.toList, id, aliasMap, messageListener)
    } yield {
      ts
    }

    teamMaster.flatMap(lst => {
      val (good, _) = lst.partition(t => t.name.trim.nonEmpty && t.nickname.trim.nonEmpty)
      logger.info(s"Saving ${good.size} teams")
      dao.saveTeams(good)
    }).map(_.size)

  }


  private def scrapeKeyList(is: Iterable[(String, String)], tag: String, aliasMap: Map[String, String], messageListener: Option[ActorRef]): Future[List[Team]] = {

    val size = is.size
    implicit val timeout: Timeout = Timeout((10 + size * 2).seconds)
    val counter = Agent(0)

    def sendProgressMessage(s: String): Unit = {
      counter.send(_ + 1)
      messageListener.foreach(out => out ! SSTaskProgress(Some(counter.get.toDouble / size), Some(s)))
    }

    val futureTeams: Iterable[Future[Option[Team]]] = is.map {
      case (key, shortName) =>
        val fot: Future[Option[Team]] = (throttler ? TeamDetail(aliasMap.getOrElse(key, key), shortName, tag))
          .mapTo[ScrapingResponse[Team]].map(_.result match {
          case Success(t) => Some(t)
          case Failure(thr) =>
            logger.warn(s"Failed scraping team key $key: ${thr.getMessage}")
            None
        })
        fot.onComplete {
          case Success(Some(team)) => sendProgressMessage(team.name)
          case Success(None) => sendProgressMessage(s"[$key]")
          case Failure(thr) => sendProgressMessage(s"$key->${thr.getMessage}")
        }
        fot
    }
    Future.sequence(futureTeams).map(_.toList.flatten)
  }


  private def loadTeamShortNames: Future[Map[String, String]] = {
    val lines: List[String] = Source.fromInputStream(getClass.getResourceAsStream("/team-keys.txt")).getLines.toList.map(_.trim).filterNot(_.startsWith("#")).filter(_.length > 0)
    val keys: Map[String, String] = lines.flatMap(l => {
      val parts = l.trim.split("\\|")
      if (parts.size == 2) {
        Some(parts(0) -> parts(1))
      } else {
        None
      }
    }).toMap
    Future.successful(keys)
  }

  private def loadAliasMap: Future[Map[String, String]] = {
    dao.listAliases.map(_.map(alias => alias.alias -> alias.key).toMap)
  }

}

