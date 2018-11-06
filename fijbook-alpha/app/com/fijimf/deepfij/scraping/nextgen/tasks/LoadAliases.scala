package com.fijimf.deepfij.scraping.nextgen.tasks

import akka.actor.ActorRef
import com.fijimf.deepfij.models.Alias
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.scraping.nextgen.SSTask

import scala.concurrent.Future
import scala.io.Source

final case class LoadAliases(dao: ScheduleDAO) extends SSTask[Int] {

  import scala.concurrent.ExecutionContext.Implicits.global

  val name: String = "Load aliases"

  def run(listener: Option[ActorRef]): Future[Int] = {

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

  val safeToRun: Future[Boolean] = Future.successful(true)

}
