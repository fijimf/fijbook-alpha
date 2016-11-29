package forms

import java.time.LocalDate

import play.api.data.Form
import play.api.data.Forms._

object EditSeasonForm {

  val form = Form(
    mapping(
      "id" -> ignored[Long](0),
      "year" -> number(2000, 2100),
      "lock" -> text(0,8).verifying(s=>List("open","locked","update").contains(s)),
      "lockBefore"->optional(localDate)
    )(Data.apply)(Data.unapply)
  )

  case class Data(id: Long, year: Int, lock:String, lockBefore:Option[LocalDate])

}