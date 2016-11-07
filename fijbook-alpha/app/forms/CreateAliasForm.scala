package forms

import play.api.data.Form
import play.api.data.Forms._

object CreateAliasForm {

  val form = Form(
    mapping(
      "id" -> ignored[Long](0),
      "alias" -> nonEmptyText,
      "key" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  case class Data(id: Long,  alias: String, key: String)

}
