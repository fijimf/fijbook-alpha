package forms

import play.api.data.Form
import play.api.data.Forms._

object EditQuoteForm {

  val form: Form[Data] = Form(
    mapping(
      "id" -> longNumber,
      "quote" -> nonEmptyText,
      "source" -> optional(text),
      "url"->optional(text),
      "key"->optional(text)
    )(Data.apply)(Data.unapply)
  )

  final case class Data(id: Long,  quote: String, source: Option[String], url: Option[String], key: Option[String])

}
