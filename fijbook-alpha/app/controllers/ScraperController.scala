package controllers

import akka.actor.ActorRef
import com.fijimf.deepfij.models.Team
import com.fijimf.deepfij.scraping.modules.scraping.requests.{ShortNameAndKeyByStatAndPage, TeamDetail}
import com.google.inject.Inject
import com.google.inject.name.Named
import play.api.Logger
import play.api.mvc.{Action, Controller}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

class ScraperController @Inject()(@Named("data-load-actor") teamLoad: ActorRef) extends Controller {
  import scala.concurrent.ExecutionContext.Implicits.global
  val logger = Logger(getClass)
  implicit val timeout = Timeout(600.seconds)

  def scrape() = Action.async {
    logger.info("Loading preliminary team keys.")
    val teamShortNames: Future[Map[String, String]] = masterShortName(List(1, 2, 3, 4, 5, 6, 7), 145)

    logger.info("Loading team detail")

    val teamMaster: Future[List[Team]] = teamShortNames.map((tsn: Map[String, String]) => {
      tsn.keys.grouped(4).map((is: Iterable[String]) => {
        Await.result(Future.sequence(is.map((k: String) => {
          (teamLoad ? TeamDetail(k, tsn(k))).mapTo[Team]
        })), 600.seconds)
      }).flatten.toList
    })
    teamMaster.map(tl=>Ok(tl.mkString("\n")))
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