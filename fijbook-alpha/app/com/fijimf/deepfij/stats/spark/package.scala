package com.fijimf.deepfij.stats

import play.api.libs.json.{Json, Writes}

package object spark {
   implicit val writesStatsClusterSummary:Writes[StatClusterSummary] = new Writes[StatClusterSummary] {
      def writes(c: StatClusterSummary) = {
        Json.obj(
          "name" -> Json.toJson(c.name),
          "status" -> Json.toJson(c.status),
          "create" -> Json.toJson(c.creation),
          "ready" -> Json.toJson(c.ready),
          "end" -> Json.toJson(c.terminated)
        )
      }
    }
}
