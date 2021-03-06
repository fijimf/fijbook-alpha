package forms

import play.api.data.Form
import play.api.data.Forms._

object EditStaticPageForm {

  val form: Form[Data] = Form(
    mapping(
      "key" -> nonEmptyText,
      "content" -> nonEmptyText
     )(Data.apply)(Data.unapply)
  )

  final case class Data(key: String, content: String)

}
