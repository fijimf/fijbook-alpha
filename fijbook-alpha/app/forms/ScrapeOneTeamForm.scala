package forms

import play.api.data.Form
import play.api.data.Forms._

object ScrapeOneTeamForm {


  val form = Form(
    mapping(
      "key" -> nonEmptyText,
      "shortName" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  case class Data(key: String, shortName: String)

}

