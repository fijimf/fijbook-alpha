package forms

import com.fijimf.deepfij.models.Game
import play.api.data.Form
import play.api.data.Forms._

//case class Game(id: Long, seasonId: Long, homeTeamId: Long, awayTeamId: Long, date: LocalDate, datetime: LocalDateTime, location: Option[String], isNeutralSite: Boolean, tourneyKey: Option[String], homeTeamSeed: Option[Int], awayTeamSeed: Option[Int], sourceKey: String, updatedAt: LocalDateTime, updatedBy: String) {

object EditGameForm {

  val form = Form(
    mapping(
      "id" -> longNumber,
      "seasonId" -> longNumber,
      "homeTeamId" -> longNumber,
      "awayTeamId" -> longNumber,
      "datetime" -> nonEmptyText,
      "location" -> optional(text),
      "isNeutral" -> boolean,
      "tourney" -> optional(text),
      "homeTeamSeed" -> optional(number(min = 1, max = 16)),
      "awayTeamSeed" -> optional(number(min = 1, max = 16)),
      "sourceKey" -> text
    )(Data.apply)(Data.unapply)
  )

  case class Data(
                   id: Long,
                   seasonId: Long,
                   homeTeamId: Long,
                   awayTeamId: Long,
                   datetime: String,
                   location: Option[String],
                   isNeutral: Boolean,
                   tourney: Option[String],
                   homeTeamSeed: Option[Int],
                   awayTeamSeed: Option[Int],
                   sourceKey: String
                 )

  def team2Data(g: Game) = {
    Data(
      g.id,
      g.seasonId,
      g.homeTeamId,
      g.awayTeamId,
      g.datetime.toString,
      g.location,
      g.isNeutralSite,
      g.tourneyKey,
      g.homeTeamSeed,
      g.awayTeamSeed,
      g.sourceKey
    )
  }
}