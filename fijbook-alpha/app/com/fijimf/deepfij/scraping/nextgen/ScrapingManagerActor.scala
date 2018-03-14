package com.fijimf.deepfij.scraping.nextgen

import java.time.LocalDateTime

import akka.actor.{Actor, ActorRef, Props}
import akka.contrib.throttle.Throttler.Rate
import akka.contrib.throttle.{Throttler, TimerBasedThrottler}
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.dao.schedule.util.ScheduleUtil
import com.fijimf.deepfij.models.services.ScheduleUpdateService
import com.fijimf.deepfij.scraping.{ScrapingActor, ScrapingResponse, TeamDetail, TestUrl}
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success}

object ScrapingManagerActor {
  def props(out: ActorRef,
            superScraper: ActorRef,
            ws: WSClient,
            repo: ScheduleRepository,
            dao: ScheduleDAO,
            schedSvc:ScheduleUpdateService
           ) = Props(new ScrapingManagerActor(out, superScraper, ws, repo, dao, schedSvc))
}

case class ScrapingManagerActor(
                                 out: ActorRef,
                                 superScraper: ActorRef,
                                 ws: WSClient,
                                 repo: ScheduleRepository,
                                 dao: ScheduleDAO,
                                 schedSvc:ScheduleUpdateService
                               ) extends Actor {

  val teamDataActor: ActorRef = context.actorOf(ScrapingActor.props(ws))
  val teamThrottler: ActorRef = context.actorOf(ScrapeThrottler.props(Rate(2, 1.second)))
  teamThrottler ! Throttler.SetTarget(Some(teamDataActor))
  val confDataActor: ActorRef = context.actorOf(ScrapingActor.props(ws))
  val confThrottler: ActorRef = context.actorOf(ScrapeThrottler.props(Rate(2, 1.second)))
  confThrottler ! Throttler.SetTarget(Some(confDataActor))
  val log = Logger(this.getClass)

  override def receive: Receive = {
    case (s: String) if s.toLowerCase == "$command::status" =>
      log.info("Received status command")
      superScraper ! SSStatus
    case (s: String) if s.toLowerCase == "$command::full_rebuild" =>
      log.info("Received full rebuild command")
      val msg = SSProcessTasks(List(
        ScrapeManagingActor.rebuildDatabase(repo),
        ScrapeManagingActor.loadAliases(dao),
        ScrapeManagingActor.loadTeams(System.currentTimeMillis().toString, dao, teamThrottler, superScraper),
        ScrapeManagingActor.scrapeConferences(System.currentTimeMillis().toString, dao, confThrottler),
        ScrapeManagingActor.createSeasons(2014,2018, dao),
        ScrapeManagingActor.seedConferenceMaps(System.currentTimeMillis().toString,dao),
        ScrapeManagingActor.scrapeGames(dao, schedSvc, System.currentTimeMillis().toString)
      ))
      superScraper ! msg
    case (r: ReadyData) => out ! Json.toJson(r).toString()
    case (p: ProcessingData) => out ! Json.toJson(p).toString()
    case (taskId: String, progress: String) => out ! Json.toJson(Map("type" -> "progress", "taskId" -> taskId, "progress" -> progress)).toString()
    case _ =>
  }
}


object ScrapeManagingActor {

  import scala.concurrent.ExecutionContext.Implicits.global

  def rebuildDatabase(repo: ScheduleRepository): SSTask[Unit] = new SSTask[Unit] {
    override def name: String = "Rebuild database"

    override def run: Future[Unit] = repo.dropSchema().flatMap(_ => repo.createSchema())

  }

  def loadAliases(dao: ScheduleDAO): SSTask[Int] = new SSTask[Int] {
    override def name: String = "Load aliases"

    override def run: Future[Int] = {
      val lines: List[String] = Source.fromInputStream(getClass.getResourceAsStream("/aliases.txt")).getLines.toList.map(_.trim).filterNot(_.startsWith("#")).filter(_.length > 0)

      val aliases = lines.flatMap(l => {
        val parts = l.trim.split("\\s+")
        if (parts.size == 2) {
          Some(Alias(0L, parts(0), parts(1)))
        } else {
          None
        }
      })
      dao.saveAliases(aliases).map(_.size)
    }

  }

  def loadTeams(tag: String, dao: ScheduleDAO, throttler: ActorRef, listener: ActorRef): SSTask[Int] = {
    new SSTask[Int] {

      private val logger = Logger(s"LoadTeams--$tag")

      override def name: String = "Load team data"

      override def run: Future[Int] = {

        val teamMaster: Future[List[Team]] = for {
          aliasMap <- loadAliasMap
          teamShortNames <- loadTeamShortNames
          ts <- scrapeKeyList(teamShortNames.toList, tag, aliasMap, listener)
        } yield {
          ts
        }

        teamMaster.flatMap(lst => {
          val (good, _) = lst.partition(t => t.name.trim.nonEmpty && t.nickname.trim.nonEmpty)
          logger.info(s"Saving ${good.size} teams")
          dao.saveTeams(good)
        }).map(_.size)

      }


      private def scrapeKeyList(is: Iterable[(String, String)], tag: String, aliasMap: Map[String, String], out: ActorRef): Future[List[Team]] = {

        implicit val timeout: Timeout = Timeout((10 + is.size * 2).seconds)
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
              case Success(Some(team)) => out ! SSTaskProgress(team.name)
              case Success(None) => out ! SSTaskProgress(":missing:")
              case Failure(thr) => out ! SSTaskProgress(thr.getMessage)
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
  }


  def scrapeConferences(tag: String, dao: ScheduleDAO, throttler: ActorRef): SSTask[Int] = new SSTask[Int] {
    override def name: String = "Scrape conferences"

    override def run: Future[Int] = {

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
        val conferences: Future[List[Conference]] = Future.sequence(names.map(n => createConference(tag, transforms, n))).map(_.toList.flatten)
        conferences.flatMap(cfs => {
          dao.saveConferences(cfs).map(_.size)

        })
      })
    }

    private def createConference(tag: String, transforms: List[(String) => String], n: String): Future[Option[Conference]] = {

      val candidate: Future[Option[String]] = findConferenceKey(transforms, n)
      candidate.map {
        case Some(s) =>
          val key = s.toLowerCase.replace(' ', '-')
          val smLogo = "http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + key + ".40.png"
          val lgLogo = "http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + key + ".70.png"
          Some(Conference(0L, key, n.replaceFirst("\\.\\.\\.$", ""), Some(lgLogo), Some(smLogo), None, None, None, lockRecord = false, LocalDateTime.now(), tag))
        case None =>
          None
      }
    }

    private def findConferenceKey(transforms: List[(String) => String], n: String): Future[Option[String]] = {
      implicit val timeout = Timeout(5.minutes)
      Future.sequence(
        transforms.map(f => f(n)).toSet.map((k: String) => {
          val u = TestUrl("http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + k + ".70.png")
          (throttler ? u).mapTo[Option[Int]].map(oi => k -> oi)
        })).map(_.filter(_._2 == Some(200)).headOption.map(_._1))
    }
  }


  def createSeasons(start: Int, end: Int, dao: ScheduleDAO): SSTask[Int] = new SSTask[Int] {

    override def name: String = "Create seasons"

    override def run: Future[Int] = {
      dao.saveSeasons(start.to(end).map(y => Season(0L, y, "", None)).toList).map(_.size)
    }
  }

  def seedConferenceMaps(tag: String, dao: ScheduleDAO): SSTask[Int] = new SSTask[Int] {
    override def run: Future[Int] = {
      for {
        _ <- dao.deleteAllConferenceMaps()
        lcm <- ScheduleUtil.createConferenceMapSeeds(dao, tag)
        _ <- dao.saveConferenceMaps(lcm)
      } yield {
        lcm.size
      }
    }

    override def name: String = "Seed conference maps"

  }


  def scrapeGames(dao: ScheduleDAO, schedSvc: ScheduleUpdateService, tag: String): SSTask[List[_]] = new SSTask[List[_]] {

    val logger = Logger(this.getClass)

    override def name: String = "Scrape games"

    override def run: Future[List[_]] = {
      dao.listSeasons.flatMap(ss => {
        Future.sequence(ss.map(s => {
          schedSvc.loadSeason(s, tag).recover {
            case thr => logger.error(s"Failed loading games for ${s.year} with ${thr.getMessage}")
          }
        }))
      })
    }
  }
}

object ScrapeThrottler {
  def props(r: Rate) = Props(classOf[TimerBasedThrottler], r)
}
