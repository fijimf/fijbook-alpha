package forms.deepfij

import java.time.LocalDate

import play.api.data.Form
import play.api.data.Forms._

object StartEndDateForm {

  val form = Form(
    mapping(
      "start" -> localDate("yyyy-MM-dd"),
      "end" -> localDate("yyyy-MM-dd")
    )(Data.apply)(Data.unapply)
  )

  case class Data(start:LocalDate,end:LocalDate)

}