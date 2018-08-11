package com.fijimf.deepfij

import play.api.libs.json.{Format, Json}

package object models {

  implicit val formatsTeam: Format[Team] = Json.format[Team]
  implicit val formatsAlias: Format[Alias] = Json.format[Alias]
  implicit val formatsConference: Format[Conference] = Json.format[Conference]
  implicit val formatsQuotes: Format[Quote] = Json.format[Quote]

}
