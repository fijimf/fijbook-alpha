package com.fijimf.deepfij.models

import play.api.libs.json.{JsObject, Json, Writes}

package object react {

  implicit val displayUserWrites: Writes[DisplayUser] = new Writes[DisplayUser] {
    def writes(u: DisplayUser): JsObject = {
      Json.obj(
        "user" -> (u.user match {
          case Some(user) => Json.obj(
            "name" -> Json.toJson(user.name),
            "email" -> Json.toJson(user.email.getOrElse(""))
          )
          case None => Json.obj(
            "name" -> Json.toJson("Guest"),
            "email" -> Json.toJson("")
          )
        }),
        "isAdmin" -> u.isAdmin,
        "isLoggedIn"-> u.isLoggedIn,
        "favorites" -> Json.arr(
          u.favorites.map(l => Json.toJson(l))
        ),
        "dailyQuotesLiked" -> Json.arr(
          u.dailyQuotesLiked
        )
      )
    }
  }

  implicit val displayLinkWrites: Writes[DisplayLink] = new Writes[DisplayLink] {
    def writes(l: DisplayLink) = {
      Json.obj("display" -> Json.toJson(l.display), "url" -> Json.toJson(l.link), "icon" -> Json.toJson(l.icon))
    }
  }

  implicit val displayPageWrites: Writes[DisplayPage] = new Writes[DisplayPage] {
    def writes(p: DisplayPage) = {
      Json.obj(
        "displayUser" -> Json.toJson(p.displayUser),
        "menu" -> Json.arr(p.menu.map(_.link)))
    }
  }

}
