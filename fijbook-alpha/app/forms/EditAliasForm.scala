package forms

import play.api.data.Form
import play.api.data.Forms._

object EditAliasForm {

  val form: Form[Data] = Form(
    mapping(
      "id" -> default(longNumber,0L),
      "alias" -> nonEmptyText,
      "key" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  final case class Data(id: Long,  alias: String, key: String)

}
