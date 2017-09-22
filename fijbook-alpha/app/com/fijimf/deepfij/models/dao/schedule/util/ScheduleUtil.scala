package com.fijimf.deepfij.models.dao.schedule.util

import java.time.LocalDateTime

import com.fijimf.deepfij.models.{Conference, ConferenceMap, Season, Team}
import com.fijimf.deepfij.models.dao.schedule.{ScheduleDAO, TeamDAO}
import play.api.Logger

import scala.concurrent.Future

object ScheduleUtil {
  val logger = Logger(getClass)

  import scala.concurrent.ExecutionContext.Implicits.global
  def createConferenceMapSeeds(dao: ScheduleDAO, userTag: String): Future[List[ConferenceMap]] = {
    for {
      sl <- dao.listSeasons
      tl <- dao.listTeams
      cl <- dao.listConferences
    } yield {
      val confNameMap = cl.map(c => c.name -> c).toMap ++
        cl.map(c => c.name.replace("Conference", "").trim -> c) ++
        cl.map(c => c.name.replace("The", "").trim -> c)
      logger.info("Conference lookup:\n"+confNameMap.toList.map(t=>t._1->t._2.key).mkString("\n"))
      logger.info(s"Creating map for ${sl.size} seasons and ${tl.size} teams")
      for {
        s <- sl
        t <- tl
      } yield {
        guessConfForTeam(userTag, confNameMap, s, t)
      }
    }
  }

  private def guessConfForTeam(userTag: String, confNameMap: Map[String, Conference], s: Season, t: Team) = {
    val nameKey = t.optConference
      .replaceFirst("Athletic Association$", "Athletic...")
      .replaceFirst("\\.\\.\\.$", "")
      .replaceFirst("American Athletic Conference", "American Athletic").trim
    if (nameKey!=t.optConference){
      logger.info(s"For ${t.name} mangling ${t.optConference} to $nameKey")
    }
    val conf = confNameMap.get(nameKey) match {
      case Some(c) => c
      case None =>
        logger.info(s"For ${t.name} failed to find a conference.  Listing Independent")
        confNameMap("Independents")
    }
    ConferenceMap(0L, s.id, conf.id, t.id, false, LocalDateTime.now(), userTag)
  }
}
