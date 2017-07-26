package forms

import play.api.data.Form
import play.api.data.Forms._

object EditStaticPageForm {

  val form = Form(
    mapping(
      "slug" -> nonEmptyText,
      "content" -> nonEmptyText
     )(Data.apply)(Data.unapply)
  )

  case class Data(slug: String, content: String)

}
