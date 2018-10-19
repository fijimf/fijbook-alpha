package forms

import com.fijimf.deepfij.models.Conference
import play.api.data.Form
import play.api.data.Forms._

object EditConferenceForm {

  val form = Form(
    mapping(
      "id" -> longNumber,
      "key" -> nonEmptyText,
      "name" -> nonEmptyText,
      "level" -> nonEmptyText,
      "logoLgUrl" -> optional(text),
      "logoSmUrl" -> optional(text),
      "officialUrl" -> optional(text),
      "officialTwitter" -> optional(text),
      "officialFacebook" -> optional(text)
    )(Data.apply)(Data.unapply)
  )

  final case class Data(
                   id: Long,
                   key: String,
                   name: String,
                   level: String,
                   logoLgUrl: Option[String],
                   logoSmUrl: Option[String],
                   officialUrl: Option[String],
                   officialTwitter: Option[String],
                   officialFacebook: Option[String]
                 )

  def conf2Data(c: Conference): Data = {
    Data(
      c.id,
      c.key,
      c.name,
      c.level,
      c.logoLgUrl,
      c.logoSmUrl,
      c.officialUrl,
      c.officialTwitter,
      c.officialFacebook
    )
  }
}