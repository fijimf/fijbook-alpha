package com.fijimf.deepfij.scraping

import java.time.LocalDateTime

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.dao.schedule.util.ScheduleUtil
import com.fijimf.deepfij.models.services.ScheduleUpdateService
import com.fijimf.deepfij.scraping.modules.scraping.requests.TeamDetail
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success}

case class UberScraper(dao: ScheduleDAO, repo: ScheduleRepository, schedSvc:ScheduleUpdateService, throttler: ActorRef) {
  val logger = Logger(getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  case class Tracking(stamp: LocalDateTime, step: String)

  implicit val timeout: Timeout = Timeout(600.seconds)


  def masterRebuild(tag: String, startYear: Int, endYear: Int) = {
    for {
      step1 <- rebuildDatabase
      step2 <- loadAliases
      step3 <- loadTeams(tag)
      step4 <- scrapeConferences(tag)
      step5 <- createSeasons(startYear, endYear)
      step6 <- seedConferenceMaps(tag)
      step7 <- scrapeGames(tag)
    //      step8 <- identifyNeutralSites
    //      step9 <- cleanUpMissedGames
    //      step10 <- verify
    } yield {
      (step1 ++ step2 ++ step3++step4++step5++step6++step7).sortBy(_.stamp.toString)
    }
  }

  def rebuildDatabase: Future[Seq[Tracking]] = {
    for {
      u1 <- repo.dropSchema().map(_ => Tracking(LocalDateTime.now(), "Dropped Schema"))
      u2 <- repo.createSchema().map(_ => Tracking(LocalDateTime.now(), "Rebuilt Schema"))
    } yield {
      Seq(u1, u2)
    }
  }

  def loadAliases = {
    val lines: List[String] = Source.fromInputStream(getClass.getResourceAsStream("/aliases.txt")).getLines.toList.map(_.trim).filterNot(_.startsWith("#")).filter(_.length > 0)
    val aliases = lines.flatMap(l => {
      val parts = l.trim.split("\\s+")
      if (parts.size == 2) {
        Some(Alias(0L, parts(0), parts(1)))
      } else {
        None
      }
    })
    dao.saveAliases(aliases).map(
      aa => Seq(Tracking(LocalDateTime.now(), s"Loaded ${aa.size} aliases"))
    )
  }

  def loadTeams(tag: String): Future[List[Tracking]] = {

    val teamMaster: Future[List[Team]] = for {
      aliasMap <- loadAliasMap
      teamShortNames <- loadTeamShortNames
      ts <- scrapeKeyList(teamShortNames.toList, tag, aliasMap)
    } yield {
      ts
    }

    teamMaster.flatMap(lst => {
      val (good, bad) = lst.partition(t => t.name.trim.nonEmpty && t.nickname.trim.nonEmpty)
      logger.info(s"Saving ${good.size} teams")
      dao.saveTeams(good)
    }).map(_.map(t => Tracking(t.updatedAt, s"Saved ${t.key}")))

  }

  private def scrapeKeyList(is: Iterable[(String, String)], tag: String, aliasMap: Map[String, String]): Future[List[Team]] = {
    val futureTeams: Iterable[Future[Option[Team]]] = is.map {
      case (key, shortName) =>
        (throttler ? TeamDetail(aliasMap.getOrElse(key, key), shortName, tag))
          .mapTo[ScrapingResponse[Team]].map(_.result match {
          case Success(t) => Some(t)
          case Failure(thr) =>
            logger.warn(s"Failed scraping team key $key")
            None
        })
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

  def scrapeConferences(tag: String): Future[Seq[Tracking]] = {

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
    logger.info("Loading preliminary team keys.")
    dao.listTeams.flatMap(teamList => {
      val names = teamList.map(_.optConference.replaceFirst("Athletic Association$", "Athletic...")).toSet
      val conferences: Future[List[Conference]] = Future.sequence(names.map(n => createConference(tag, transforms, n))).map(_.toList.flatten)
      conferences.flatMap(cfs => {
        dao.saveConferences(cfs).map(cs => List(Tracking(LocalDateTime.now(), s"Loaded ${cs.size}")))

      })
    })
  }

  private def createConference(tag:String, transforms: List[(String) => String], n: String): Future[Option[Conference]] = {

    val candidate: Future[Option[String]] = findConferenceKey(transforms, n)
    candidate.map {
      case Some(s) =>
        val key = s.toLowerCase.replace(' ', '-')
        val smLogo = "http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + key + ".40.png"
        val lgLogo = "http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + key + ".70.png"
        Some(Conference(0L, key, n.replaceFirst("\\.\\.\\.$", ""), Some(lgLogo), Some(smLogo), None, None, None, false, LocalDateTime.now(), tag))
      case None =>
        None
    }

  }

  private def findConferenceKey(transforms: List[(String) => String], n: String): Future[Option[String]] = {
    Future.sequence(
      transforms.map(f => f(n)).toSet.map((k: String) => {
        logger.info("Trying " + k)
        val u = TestUrl("http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + k + ".70.png")
        (throttler ? u).mapTo[Option[Int]].map(oi => k -> oi)
      })).map(_.filter(_._2 == Some(200)).headOption.map(_._1))
  }

  def createSeasons(start: Int, end: Int) = {
    dao.saveSeasons(start.to(end).map(y => Season(0L, y, "", None)).toList).map(ss => List(Tracking(LocalDateTime.now(), s"Saved ${ss.size} seasons")))
  }
  def seedConferenceMaps(tag:String) = {
    for {
      _ <- dao.deleteAllConferenceMaps()
      lcm <- ScheduleUtil.createConferenceMapSeeds(dao, tag)
      _ <- dao.saveConferenceMaps(lcm)
    } yield {
      List(Tracking(LocalDateTime.now(), s"Saved ${lcm.size}"))
    }
  }

  def scrapeGames(tag:String): Future[List[Tracking]] = {
    dao.listSeasons.flatMap(ss=>{
      Future.sequence(ss.map(s=>{
        schedSvc.loadSeason(s, tag).map(_=>Tracking(LocalDateTime.now(),s"Loaded games for ${s.year}"))
      }))
    })
  }
}
