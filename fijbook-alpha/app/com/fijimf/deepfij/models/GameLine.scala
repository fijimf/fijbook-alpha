package com.fijimf.deepfij.models

import java.time.LocalDate
import java.time.format.DateTimeFormatter

final case class GameLine(date:LocalDate, vsAt:String, opp:Team, wl:String, literalScore:String) {
  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MMM-yy")
  val longDate: String =date.format(formatter)
  private val linkDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
  val dateLink:String = controllers.routes.ReactMainController.dateIndex(date.format(linkDateFormat)).url
}