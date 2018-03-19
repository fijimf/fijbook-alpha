package com.fijimf.deepfij.scraping

import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDateTime}

import play.api.libs.json.{Json, Writes}

package object nextgen {

  implicit val readyDataWrites: Writes[ReadyData] = new Writes[ReadyData] {
    def writes(r: ReadyData) = {
      Json.obj(
        "type" -> Json.toJson("ready"),
        "status" -> Json.toJson(r.last.map(_.status).getOrElse("Unknown")),
        "lastTaskList" -> Json.toJson(r.last.toList))
    }
  }

  implicit val ssTaskListMetadata: Writes[SSTaskListMetadata] = new Writes[SSTaskListMetadata] {
    def writes(tlm: SSTaskListMetadata) = {
      Json.obj("id" -> Json.toJson(tlm.id), "tasks" -> Json.toJson(tlm.tasks))
    }
  }

  implicit val ssTaskMetaData: Writes[SSTaskMetaData] = new Writes[SSTaskMetaData] {
    def writes(x: SSTaskMetaData) = {
      x match {
        case SSRunningTaskMetadata(id, name, startedAt) =>
          Json.obj(
            "id" -> Json.toJson(id),
            "name" -> Json.toJson(name),
            "startedAt" -> Json.toJson(startedAt.format(DateTimeFormatter.ISO_LOCAL_TIME)),
            "completedAt" -> Json.toJson(""),
            "elapsedTime" -> FormatDuration(Duration.between(startedAt, LocalDateTime.now)),
            "message" -> Json.toJson("Running")
          )
        case SSCompletedTaskMetadata(id, name, startedAt, completedAt, _) =>
          Json.obj(
            "id" -> Json.toJson(id),
            "name" -> Json.toJson(name),
            "startedAt" -> Json.toJson(startedAt.format(DateTimeFormatter.ISO_LOCAL_TIME)),
            "completedAt" -> Json.toJson(completedAt.format(DateTimeFormatter.ISO_LOCAL_TIME)),
            "elapsedTime" -> FormatDuration(Duration.between(startedAt, completedAt)),
            "message" -> Json.toJson("Completed")
          )
        case SSFailedTaskMetadata(id, name, startedAt, completedAt, thr) =>
          Json.obj(
            "id" -> Json.toJson(id),
            "name" -> Json.toJson(name),
            "startedAt" -> Json.toJson(startedAt.format(DateTimeFormatter.ISO_LOCAL_TIME)),
            "completedAt" -> Json.toJson(completedAt.format(DateTimeFormatter.ISO_LOCAL_TIME)),
            "elapsedTime" -> FormatDuration(Duration.between(startedAt, completedAt)),
            "message" -> Json.toJson(s"Failed -> ${thr.getMessage}")
          )
        case SSAbortedTaskMetadata(id, name, abortedAt) =>
          Json.obj(
            "id" -> Json.toJson(id),
            "name" -> Json.toJson(name),
            "startedAt" -> Json.toJson(abortedAt.format(DateTimeFormatter.ISO_LOCAL_TIME)),
            "completedAt" -> Json.toJson(abortedAt.format(DateTimeFormatter.ISO_LOCAL_TIME)),
            "elapsedTime" -> "~",
            "message" -> Json.toJson(s"Aborted")
          )
      }
    }
  }

  implicit val writesProcessingData: Writes[ProcessingData] = new Writes[ProcessingData] {
    def writes(p: ProcessingData) = {
      Json.obj(
        "type" -> Json.toJson("running"),
        "running" -> Json.toJson(p.runningTask),
        "completed" -> Json.toJson(p.completedTasks),
        "toRun" -> Json.toJson(p.tasksToRun)
      )
    }
  }

  implicit val writesTask: Writes[SSTask[_]] = new Writes[SSTask[_]] {
    def writes(t: SSTask[_]) = {
      Json.obj(
        "name" -> Json.toJson(t.name)
      )
    }
  }
}

object FormatDuration {
  def apply(duration: Duration): String = {
    val millis = duration.toMillis
    val seconds = millis / 1000L
    val absSeconds = Math.abs(seconds)
    val positive = "%d:%02d:%02d.%03d".format(absSeconds / 3600, (absSeconds % 3600) / 60, absSeconds % 60, millis % 1000)
    if (seconds < 0) "-" + positive
    else positive
  }
}