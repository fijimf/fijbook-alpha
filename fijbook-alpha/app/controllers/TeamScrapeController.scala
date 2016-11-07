package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.scraping.modules.scraping.requests.{ShortNameAndKeyByStatAndPage, TeamDetail}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TeamScrapeController @Inject()(@Named("data-load-actor") teamLoad: ActorRef, val teamDao: TeamDAO, silhouette: Silhouette[DefaultEnv]) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)
  implicit val timeout = Timeout(600.seconds)

  def scrapeTeams() = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Loading aliases from database")
    val aliasMap = Await.result(teamDao.aliasList.map(_.map(alias=>alias.alias->alias.key)),600.seconds)

    logger.info("Loading preliminary team keys.")
    val teamShortNames: Future[Map[String, String]] = masterShortName(List(1, 2, 3, 4, 5, 6, 7), 145)

    logger.info("Loading team detail")

    val teamMaster: Future[List[Team]] = teamShortNames.map((tsn: Map[String, String]) => {
      tsn.toList.grouped(4).map((is: Iterable[(String,String)]) => {
        scrapeKeyList(is, "Scraper["+rs.identity.name.getOrElse("???")+"]")
      }).flatten.toList
    })

    teamMaster.flatMap(lst => {
      val (good, bad) = lst.partition(t => t.name.trim.nonEmpty && t.nickname.trim.nonEmpty)
      good.foreach(t => {
        logger.info("Saving " + t.key)
        teamDao.save(t)
      })
      Future {
        val badTeamlist: String = bad.map(_.key).mkString("\n")
        logger.info("The following teams were bad:\n" + badTeamlist)
        Ok(badTeamlist)
      }
    })

  }

  def scrapeKeyList(is: Iterable[(String, String)], userTag: String): Iterable[Team] = {
    Await.result(Future.sequence(is.map {
      case (key, shortName) => {
        (teamLoad ? TeamDetail(key, shortName, userTag)).mapTo[Team]
      }
    }), 600.seconds)
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