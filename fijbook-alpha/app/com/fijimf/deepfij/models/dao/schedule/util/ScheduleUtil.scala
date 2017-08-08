package com.fijimf.deepfij.models.dao.schedule.util

import java.time.LocalDateTime

import com.fijimf.deepfij.models.ConferenceMap
import com.fijimf.deepfij.models.dao.schedule.{ScheduleDAO, TeamDAO}

import scala.concurrent.Future

object ScheduleUtil {

  import scala.concurrent.ExecutionContext.Implicits.global
  def createConferenceMapSeeds(dao: ScheduleDAO, userTag: String): Future[List[ConferenceMap]] = {
    for {
      sl <- dao.listSeasons
      tl <- dao.listTeams
      cl <- dao.listConferences
    } yield {
      val confNameMap = cl.map(c => c.name -> c).toMap ++ cl.map(c => c.name.replace("Conference", "").trim -> c) ++ cl.map(c => c.name.replace("The", "").trim -> c)
      for (
        s <- sl;
        t <- tl
      ) yield {
        val nameKey = t.optConference.replaceFirst("Athletic Association$", "Athletic...").replaceFirst("\\.\\.\\.$", "")
        val conf = confNameMap.get(nameKey).orElse(confNameMap.get("Independents"))
        ConferenceMap(0L, s.id, conf.map(_.id).getOrElse(0L), t.id, false, LocalDateTime.now(), userTag)
      }
    }
  }

}
