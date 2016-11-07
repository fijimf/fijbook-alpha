package forms.deepfij

import java.time.LocalDate

import play.api.data.Form
import play.api.data.Forms._

object OneDateForm {

  val form = Form(
    mapping(
      "date" -> localDate("yyyy-MM-dd")
    )(Data.apply)(Data.unapply)
  )

  case class Data(date:LocalDate)

}