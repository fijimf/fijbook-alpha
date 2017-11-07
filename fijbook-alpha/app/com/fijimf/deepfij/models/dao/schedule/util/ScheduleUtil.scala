package com.fijimf.deepfij.models.dao.schedule.util

import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{Conference, ConferenceMap, Season, Team}
import play.api.Logger
import org.apache.commons.text.similarity.LevenshteinDistance
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
      logger.info("Conference lookup:\n" + confNameMap.toList.map(t => t._1 -> t._2.key).mkString("\n"))
      logger.info(s"Creating map for ${sl.size} seasons and ${tl.size} teams")
      for {
        s <- sl
        t <- tl
      } yield {
        guessConfForTeam(userTag, confNameMap, s, t)
      }
    }
  }

  private def guessConfForTeam(userTag: String, confNameMap: Map[String, Conference], s: Season, t: Team): ConferenceMap = {
    val nameKey = t.optConference
      .replaceFirst("Athletic Association$", "Athletic...")
      .replaceFirst("\\.\\.\\.$", "")
      .replaceFirst("American Athletic Conference", "American Athletic").trim
      .replaceFirst("America East Conference", "America East").trim
    if (nameKey != t.optConference) {
      logger.info(s"For ${t.name} mangling ${t.optConference} to $nameKey")
    }
    confNameMap.get(nameKey) match {
      case Some(c) => c
        ConferenceMap(0L, s.id, c.id, t.id, lockRecord = false, LocalDateTime.now(), userTag)
      case None =>
        logger.warn(s"For team ${t.key}, conference suggested was ${t.optConference}, mangled to $nameKey was not found in keys.  Taking a real guess")
        val k = confNameMap.keys.minBy(LevenshteinDistance.getDefaultInstance.apply(_,nameKey))
        val conference = confNameMap(k)
        logger.info(s"Result of guessing is ${conference.name}.")
        ConferenceMap(0L, s.id, conference.id, t.id, lockRecord = false, LocalDateTime.now(), userTag)
    }
  }
}
