package forms

import play.api.data.Form
import play.api.data.Forms._

object EditRssFeedForm {

  val form = Form(
    mapping(
      "id" -> default(longNumber, 0L),
      "name" -> nonEmptyText,
      "url" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  final case class Data(id: Long, name: String, url: String)

}
