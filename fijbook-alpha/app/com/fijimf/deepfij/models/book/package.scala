package com.fijimf.deepfij.models

import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDateTime}

import play.api.libs.json.{JsValue, Json, Writes}

package object book {

  implicit val writesApp: Writes[App] = new Writes[App] {
    override def writes(o: App): JsValue = Json.obj(
      "name" -> Json.toJson(o.name),
      "currentTime" -> Json.toJson(o.time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
      "openWindow" -> Json.toJson(o.openWindow.toString),
      "closeWindow" -> Json.toJson(o.closeWindow.toString),
      "bettors" -> Json.toJson(o.bettors),
      "books" -> Json.toJson(o.books)
    )
  }

  implicit val writesBettor: Writes[Bettor] = new Writes[Bettor] {
    override def writes(o: Bettor): JsValue = Json.obj(
      "name" -> Json.toJson(o.name),
      "balance" -> Json.toJson(o.balance)
    )
  }

  implicit val writesBook: Writes[Book] = new Writes[Book] {
    override def writes(o: Book): JsValue = Json.obj(
      "id" -> Json.toJson(o.game.key)
    )
  }

}
