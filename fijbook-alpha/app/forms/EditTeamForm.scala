package forms

import com.fijimf.deepfij.models.Team
import play.api.data.Form
import play.api.data.Forms._

/**
  * The form which handles the sign up process.
  */
object EditTeamForm {

  val form: Form[Data] = Form(
    mapping(
      "id" -> longNumber,
      "key" -> nonEmptyText,
      "name" -> nonEmptyText,
      "longName" -> nonEmptyText,
      "nickname" -> nonEmptyText,
      "optConference" -> nonEmptyText,
      "logoLgUrl" -> optional(text),
      "logoSmUrl" -> optional(text),
      "primaryColor" -> optional(text),
      "secondaryColor" -> optional(text),
      "officialUrl" -> optional(text),
      "officialTwitter" -> optional(text),
      "officialFacebook" -> optional(text)
    )(Data.apply)(Data.unapply)
  )

  final case class Data(
                   id: Long,
                   key: String,
                   name: String,
                   longName: String,
                   nickname: String,
                   optConference: String,
                   logoLgUrl: Option[String],
                   logoSmUrl: Option[String],
                   primaryColor: Option[String],
                   secondaryColor: Option[String],
                   officialUrl: Option[String],
                   officialTwitter: Option[String],
                   officialFacebook: Option[String]
                 )
  def team2Data(t:Team): Data = {
    Data(
      t.id,
      t.key,
      t.name,
      t.longName,
      t.nickname,
      t.optConference,
      t.logoLgUrl,
      t.logoSmUrl,
      t.primaryColor,
      t.secondaryColor,
      t.officialUrl,
      t.officialTwitter,
      t.officialFacebook
    )
  }
}