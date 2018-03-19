package com.fijimf.deepfij.scraping.nextgen.tasks

import java.util.UUID

import akka.actor.ActorRef
import com.fijimf.deepfij.models.ScheduleRepository
import com.fijimf.deepfij.scraping.nextgen.SSTask

import scala.concurrent.Future

case class RebuildDatabase(repo: ScheduleRepository) extends SSTask[Unit] {
  import scala.concurrent.ExecutionContext.Implicits.global

  val name: String = "RebuildDatabase"

  val safeToRun: Future[Boolean] = Future.successful(true)

  def run(messageListener: Option[ActorRef] = None): Future[Unit] = repo.dropSchema().flatMap(_ => repo.createSchema())


}
