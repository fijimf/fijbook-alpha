package forms

import play.api.data.Form
import play.api.data.Forms._

object CreateQuoteForm {

  val form = Form(
    mapping(
      "id" -> ignored[Long](0),
      "quote" -> nonEmptyText,
      "source" -> optional(text),
      "url"->optional(text)
    )(Data.apply)(Data.unapply)
  )

  case class Data(id: Long,  quote: String, source: Option[String], url: Option[String])

}
