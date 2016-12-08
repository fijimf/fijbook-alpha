package forms

import play.api.data.Form
import play.api.data.Forms._

object EditQuoteForm {

  val form = Form(
    mapping(
      "id" -> longNumber,
      "quote" -> nonEmptyText,
      "source" -> optional(text),
      "url"->optional(text)
    )(Data.apply)(Data.unapply)
  )

  case class Data(id: Long,  quote: String, source: Option[String], url: Option[String])

}
