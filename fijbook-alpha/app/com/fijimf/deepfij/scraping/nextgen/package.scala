package com.fijimf.deepfij.scraping

import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDateTime}

import akka.stream.actor.WatermarkRequestStrategy
import play.api.libs.json.{JsValue, Json, Writes}

package object nextgen {
  
  implicit val readyDataWrites: Writes[ReadyData] = new Writes[ReadyData] {
    def writes(r: ReadyData) = {
      Json.obj(
        "type"->Json.toJson("ready"),
        "status"->Json.toJson(r.last.map(_.status).getOrElse("Unknown")),
        "lastTaskList" -> Json.toJson(r.last.toList))
    }
  }

  implicit val ssTaskListMetadata: Writes[SSTaskListMetadata] =  new Writes[SSTaskListMetadata] {
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
            "startedAt" -> Json.toJson(startedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
            "completedAt" -> Json.toJson(""),
            "elapsedTime" -> Duration.between(startedAt, LocalDateTime.now).toString,
            "message" -> Json.toJson("Running")
          )
        case SSCompletedTaskMetadata(id, name, startedAt, completedAt, _) =>
          Json.obj(
            "id" -> Json.toJson(id),
            "name" -> Json.toJson(name),
            "startedAt" -> Json.toJson(startedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
            "completedAt" -> Json.toJson(completedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
            "elapsedTime" -> Duration.between(startedAt, completedAt).toString,
            "message" -> Json.toJson("Completed")
          )
        case SSFailedTaskMetadata(id, name, startedAt, completedAt, thr) =>
          Json.obj(
            "id" -> Json.toJson(id),
            "name" -> Json.toJson(name),
            "startedAt" -> Json.toJson(startedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
            "completedAt" -> Json.toJson(completedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
            "elapsedTime" -> Duration.between(startedAt, completedAt).toString,
            "message" -> Json.toJson(s"Failed -> ${thr.getMessage}")
          )
        case SSAbortedTaskMetadata(id, name, abortedAt) =>
          Json.obj(
            "id" -> Json.toJson(id),
            "name" -> Json.toJson(name),
            "startedAt" -> Json.toJson(abortedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
            "completedAt" -> Json.toJson(abortedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
            "elapsedTime" -> "",
            "message" -> Json.toJson(s"Aborted")
          )
      }
    }
  }

  implicit val writesProcessingData: Writes[ProcessingData] = new Writes[ProcessingData] {
    def writes(p: ProcessingData) = {
      Json.obj(
        "type"->Json.toJson("running"),
        "running"->Json.toJson(p.runningTask),
        "completed"->Json.toJson(p.completedTasks),
        "toRun"->Json.toJson(p.tasksToRun)
      )
    }
  }   
  
  implicit val writesTask: Writes[SSTask[_]] = new Writes[SSTask[_]] {
    def writes(t: SSTask[_]) = {
      Json.obj(
        "name"->Json.toJson(t.name)
      )
    }
  } 

}
