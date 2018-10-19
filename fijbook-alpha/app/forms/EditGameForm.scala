package forms

import java.time.LocalDateTime

import com.fijimf.deepfij.models.{Game, Result}
import play.api.data.Form
import play.api.data.Forms._

//final case class Game(id: Long, seasonId: Long, homeTeamId: Long, awayTeamId: Long, date: LocalDate, datetime: LocalDateTime, location: Option[String], isNeutralSite: Boolean, tourneyKey: Option[String], homeTeamSeed: Option[Int], awayTeamSeed: Option[Int], sourceKey: String, updatedAt: LocalDateTime, updatedBy: String) {

object EditGameForm {

  val form: Form[Data] = Form(
    mapping(
      "id" -> longNumber,
      "seasonId" -> longNumber,
      "homeTeamId" -> longNumber,
      "homeTeamScore" -> optional(number),
      "awayTeamId" -> longNumber,
      "awayTeamScore" -> optional(number),
      "numPeriods" -> optional(number),
      "datetime" -> nonEmptyText,
      "location" -> optional(text),
      "isNeutral" -> boolean,
      "tourney" -> optional(text),
      "sourceKey" -> text
    )(Data.apply)(Data.unapply)
  )

  final case class Data(
                   id: Long,
                   seasonId: Long,
                   homeTeamId: Long,
                   homeScore: Option[Int],
                   awayTeamId: Long,
                   awayScore: Option[Int],
                   periods: Option[Int],
                   datetime: String,
                   location: Option[String],
                   isNeutral: Boolean,
                   tourney: Option[String],
                   sourceKey: String
                 ) {
    def toGameResult(user: String): (Game, Option[Result]) = {
      val gametime = LocalDateTime.parse(datetime)
      (Game(id, seasonId, homeTeamId, awayTeamId, gametime.toLocalDate, gametime, location, isNeutral, tourney, None, None, sourceKey, LocalDateTime.now(), user),
        for {
          hts <- homeScore
          ats <- awayScore
        } yield {
          Result(0L, id, hts, ats, periods.getOrElse(2), LocalDateTime.now(), user)
        })
    }
  }

  def team2Data(g: Game, r: Option[Result]): Data = {
    Data(
      g.id,
      g.seasonId,
      g.homeTeamId,
      r.map(_.homeScore),
      g.awayTeamId,
      r.map(_.awayScore),
      r.map(_.periods),
      g.datetime.toString,
      g.location,
      g.isNeutralSite,
      g.tourneyKey,
      g.sourceKey
    )
  }
}