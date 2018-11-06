package com.fijimf.deepfij

import java.time.LocalDateTime

import play.api.libs.json.Reads._
import play.api.libs.json._

package object models {

  implicit val formatsTeam: Format[Team] = Json.format[Team]
  implicit val formatsAlias: Format[Alias] = Json.format[Alias]
  implicit val readsConference: Reads[Conference] = for {
    id <- (JsPath \ "id").read[Long]
    key <- (JsPath \ "key").read[String]
    name <- (JsPath \ "name").read[String]
    level <- (JsPath \ "level").readNullable[String]
    logoLgUrl <- (JsPath \ "logoLgUrl").readNullable[String]
    logoSmUrl <- (JsPath \ "logoSmUrl").readNullable[String]
    officialUrl <- (JsPath \ "officialUrl").readNullable[String]
    officialTwitter <- (JsPath \ "officialTwitter").readNullable[String]
    officialFacebook <- (JsPath \ "officialFacebook").readNullable[String]
    updatedAt <- (JsPath \ "updatedAt").read[LocalDateTime]
    updatedBy <- (JsPath \ "updatedBy").read[String]
  } yield {
    Conference(id, key, name, level.getOrElse("Unknown"), logoLgUrl, logoSmUrl, officialUrl, officialTwitter, officialFacebook, updatedAt, updatedBy)
  }
  implicit val writesConference: OWrites[Conference] = Json.writes[Conference]

  implicit val formatsQuotes: Format[Quote] = Json.format[Quote]

}
