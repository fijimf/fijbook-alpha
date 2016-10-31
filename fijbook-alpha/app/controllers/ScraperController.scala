package controllers

import akka.actor.ActorRef
import com.fijimf.deepfij.models.{ScheduleRepository, Team, TeamDAO}
import com.fijimf.deepfij.scraping.modules.scraping.requests.{ShortNameAndKeyByStatAndPage, TeamDetail}
import com.google.inject.Inject
import com.google.inject.name.Named
import play.api.Logger
import play.api.mvc.{Action, Controller}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import com.mohiva.play.silhouette.api.Silhouette
import utils.DefaultEnv

class ScraperController @Inject()(@Named("data-load-actor") teamLoad: ActorRef, val teamDao:TeamDAO, silhouette: Silhouette[DefaultEnv]) extends Controller {
  import scala.concurrent.ExecutionContext.Implicits.global
  val logger = Logger(getClass)
  implicit val timeout = Timeout(600.seconds)

  def scrapeTeams() = silhouette.SecuredAction.async { implicit rs=>
    logger.info("Loading preliminary team keys.")
    val teamShortNames: Future[Map[String, String]] = masterShortName(List(1, 2, 3, 4, 5, 6, 7), 145)

    logger.info("Loading team detail")

    val teamMaster: Future[List[Team]] = teamShortNames.map((tsn: Map[String, String]) => {
      tsn.keys.grouped(4).map((is: Iterable[String]) => {
        Await.result(Future.sequence(is.map((k: String) => {
          (teamLoad ? TeamDetail(k, tsn(k),"Scraper["+rs.identity.userID.toString+"]")).mapTo[Team]
        })), 600.seconds)
      }).flatten.toList
    })

    teamMaster.flatMap(lst=>{
      val (good, bad) = lst.partition(t=>t.name.trim.nonEmpty && t.nickname.trim.nonEmpty)
      good.foreach(t=>{
      logger.info("Saving "+t.key)
        teamDao.save(t)
      })
      Future{
        val badTeamlist: String = bad.map(_.key).mkString("\n")
        logger.info("The following teams were bad:\n"+badTeamlist)
        Ok(badTeamlist)}
    })

  }

  def masterShortName(pagination: List[Int], stat: Int): Future[Map[String, String]] = {
    pagination.foldLeft(Future.successful(Seq.empty[(String, String)]))((data: Future[Seq[(String, String)]], p: Int) => {
      for (
        t0 <- data;
        t1 <- (teamLoad ? ShortNameAndKeyByStatAndPage(stat, p)).mapTo[Seq[(String, String)]]
      ) yield t0 ++ t1
    }).map(_.toMap)
  }
}