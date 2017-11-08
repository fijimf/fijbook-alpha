package forms

import play.api.data.Form
import play.api.data.Forms._

object BulkEditQuoteForm {

  val form = Form(
    mapping(
      "quotes" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  case class Data(quotes: String)

}
