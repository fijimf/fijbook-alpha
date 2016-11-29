package forms

import com.fijimf.deepfij.models.Team
import play.api.data.Form
import play.api.data.Forms._

/**
  * The form which handles the sign up process.
  */
object EditConferenceForm {

  val form = Form(
    mapping(
      "id" -> longNumber,
      "key" -> nonEmptyText,
      "name" -> nonEmptyText,
      "logoLgUrl" -> optional(text),
      "logoSmUrl" -> optional(text),
      "officialUrl" -> optional(text),
      "officialTwitter" -> optional(text),
      "officialFacebook" -> optional(text)
    )(Data.apply)(Data.unapply)
  )

  case class Data(
                   id: Long,
                   key: String,
                   name: String,
                   logoLgUrl: Option[String],
                   logoSmUrl: Option[String],
                   officialUrl: Option[String],
                   officialTwitter: Option[String],
                   officialFacebook: Option[String]
                 )
  def team2Data(t:Team) = {
    Data(
      t.id,
      t.key,
      t.name,
      t.logoLgUrl,
      t.logoSmUrl,
      t.officialUrl,
      t.officialTwitter,
      t.officialFacebook
    )
  }
}