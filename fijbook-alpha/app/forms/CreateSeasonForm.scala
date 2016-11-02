package forms

import play.api.data.Form
import play.api.data.Forms._

object CreateSeasonForm {

  val form = Form(
    mapping(
      "id" -> ignored[Long](0),
      "year" -> number(2000, 2100)
    )(Data.apply)(Data.unapply)
  )

  case class Data(id: Long, year: Int)

}