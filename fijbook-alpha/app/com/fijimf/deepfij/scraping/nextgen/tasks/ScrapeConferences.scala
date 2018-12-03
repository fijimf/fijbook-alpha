package com.fijimf.deepfij.scraping.nextgen.tasks

import java.time.LocalDateTime

import akka.actor.ActorRef
import akka.agent.Agent
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models.Conference
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.scraping.TestUrl
import com.fijimf.deepfij.scraping.nextgen.{SSTask, SSTaskProgress}
import cats.implicits._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}


final case class ScrapeConferences(tag: String, dao: ScheduleDAO, throttler: ActorRef)extends SSTask[Int] {

  import scala.concurrent.ExecutionContext.Implicits.global

  val name: String = "Scrape conferences"

  def run(messageListener:Option[ActorRef]): Future[Int] =  {
    val counter = Agent(0)
    val basicKey: String => String = _.toLowerCase.replace(' ', '-')
    val dropThe: String => String = basicKey.andThen(_.replaceFirst("^the\\-", ""))
    val dropConference: String => String = basicKey.andThen(_.replaceFirst("\\-conference$", ""))
    val dropLeague: String => String = basicKey.andThen(_.replaceFirst("\\-league$", ""))
    val dropEllipsis: String => String = basicKey.andThen(_.replaceFirst("\\.\\.\\.$", ""))
    val tryConf: String => String = basicKey.andThen(_.replaceFirst("\\-athletic\\.\\.\\.$", "-athletic-conference"))
    val dropAthletic: String => String = basicKey.andThen(_.replaceFirst("\\-athletic\\.\\.\\.$", ""))
    val initials: String => String = basicKey.andThen(s => new String(s.split('-').map(_.charAt(0))))
    val tryAthletic: String => String = basicKey.andThen(_.replaceFirst("\\.\\.\\.$", "-athletic"))

    val transforms = List(basicKey, dropThe, dropConference, dropThe.andThen(dropConference), dropLeague, dropThe.andThen(dropLeague), initials, dropThe.andThen(initials), dropEllipsis, tryConf, dropAthletic, tryAthletic)
    dao.listTeams.flatMap(teamList => {
      val names = teamList.map(_.optConference.replaceFirst("Athletic Association$", "Athletic...")).toSet
      val conferences: Future[List[Conference]] = Future.sequence(names.map(n => {
        val f = createConference(tag, transforms, n)
        f.onComplete{
          case Success(conf)=>
            counter.send(_+1)
            messageListener.foreach(_ ! SSTaskProgress(Some(.95*counter.get/names.size), conf.map(_.key)))
          case Failure(thr)=>
            counter.send(_+1)
            messageListener.foreach(_ ! SSTaskProgress(Some(.95*counter.get/names.size), Some(thr.getMessage)))
        }
        f
      })).map(_.toList.flatten)
      conferences.flatMap(cfs => {
        dao.saveConferences(cfs).map(_.size)

      })
    })
  }

  private def createConference(tag: String, transforms: List[String => String], n: String): Future[Option[Conference]] = {

    val candidate: Future[Option[String]] = findConferenceKey(transforms, n)
    candidate.map {
      case Some(s) =>
        val key = s.toLowerCase.replace(' ', '-')
        val smLogo = "http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + key + ".40.png"
        val lgLogo = "http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + key + ".70.png"
        Some(Conference(0L, key, n.replaceFirst("\\.\\.\\.$", ""),"Unknown", Some(lgLogo), Some(smLogo), None, None, None, LocalDateTime.now(), tag))
      case None =>
        None
    }
  }

  private def findConferenceKey(transforms: List[String => String], n: String): Future[Option[String]] = {
    implicit val timeout: Timeout = Timeout(5.minutes)
    Future.sequence(
      transforms.map(f => f(n)).toSet.map((k: String) => {
        val u = TestUrl("http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + k + ".70.png")
        (throttler ? u).mapTo[Option[Int]].map(oi => k -> oi)
      })).map(_.toList).map(_.find(_._2 === Some(200)).map(_._1))
  }

 val safeToRun: Future[Boolean] =Future.successful(true)
}




