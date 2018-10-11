package com.fijimf.deepfij.models

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import akka.actor.{ActorContext, ActorSelection}
import javax.inject.Inject
import org.quartz.{JobKey, TriggerKey}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.DatabaseConfig
import slick.jdbc.{JdbcBackend, JdbcProfile}
import slick.lifted._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

case class Season(id: Long, year: Int) {
  val startDate: LocalDate = Season.startDate(year)
  val endDate: LocalDate = Season.endDate(year)
  val dates: List[LocalDate] = Season.dates(year)

  def canUpdate(d: LocalDate): Boolean = !(d.isBefore(startDate) || d.isAfter(endDate))
  def isInSeason(d: LocalDate): Boolean = !(d.isBefore(startDate) || d.isAfter(endDate))
}

case object Season {
  def startDate(y: Int): LocalDate = LocalDate.of(y - 1, 11, 1)

  def endDate(y: Int): LocalDate = LocalDate.of(y, 4, 30)

  def dates(y: Int): List[LocalDate] = Iterator.iterate(startDate(y)) {
    _.plusDays(1)
  }.takeWhile(_.isBefore(endDate(y))).toList
}

case class Conference(id: Long, key: String, name: String, level:String = "Unknown", logoLgUrl: Option[String], logoSmUrl: Option[String], officialUrl: Option[String], officialTwitter: Option[String], officialFacebook: Option[String], updatedAt: LocalDateTime, updatedBy: String) {
  private val strengthMap = Map("High Major" -> 3, "Mid Major" -> 2, "Low Major" -> 1, "Unknown" -> 0)
  require(strengthMap.contains(level))
  val strength: Int =strengthMap(level)
  def sameData(c: Conference): Boolean = (key == c.key
    && name == c.name
    && level == c.level
    && logoLgUrl == c.logoLgUrl
    && logoSmUrl == c.logoSmUrl
    && officialUrl == c.officialUrl
    && officialTwitter == c.officialTwitter
    && officialFacebook == c.officialFacebook
    )

}

case class GameSignature(dateHash: Int, homeId: Long, awayId: Long)

case class Game(id: Long, seasonId: Long, homeTeamId: Long, awayTeamId: Long, date: LocalDate, datetime: LocalDateTime, location: Option[String], isNeutralSite: Boolean, tourneyKey: Option[String], homeTeamSeed: Option[Int], awayTeamSeed: Option[Int], sourceKey: String, updatedAt: LocalDateTime, updatedBy: String) {

  def signature: GameSignature = GameSignature(date.hashCode(), homeTeamId, awayTeamId)

  def sameData(g: Game): Boolean = (g.seasonId == seasonId
    && g.homeTeamId == homeTeamId
    && g.awayTeamId == awayTeamId
    && g.date == date
    && g.datetime == datetime
    && g.location == location
    && g.isNeutralSite == isNeutralSite
    )

}

case class Team(id: Long, key: String, name: String, longName: String, nickname: String, optConference: String, logoLgUrl: Option[String], logoSmUrl: Option[String], primaryColor: Option[String], secondaryColor: Option[String], officialUrl: Option[String], officialTwitter: Option[String], officialFacebook: Option[String], updatedAt: LocalDateTime, updatedBy: String) extends Ordering[Team] {
  override def compare(x: Team, y: Team): Int = x.name.compare(y.name)


  def sameData(t: Team): Boolean = (key == t.key
    && name == t.name
    && longName == t.longName
    && nickname == t.nickname
    && optConference == t.optConference
    && logoLgUrl == t.logoLgUrl
    && logoSmUrl == t.logoSmUrl
    && primaryColor == t.primaryColor
    && secondaryColor == t.secondaryColor
    && officialUrl == t.officialUrl
    && officialTwitter == t.officialTwitter
    && officialFacebook == t.officialFacebook
    )


}

case class Alias(id: Long, alias: String, key: String)

case class Result(id: Long, gameId: Long, homeScore: Int, awayScore: Int, periods: Int, updatedAt: LocalDateTime, updatedBy: String) {

  def sameData(r: Result): Boolean = (r.gameId == gameId
    && r.homeScore == homeScore
    && r.awayScore == awayScore
    && r.periods == periods
    )

  def margin: Int = Math.abs(homeScore - awayScore)

  def isHomeWinner: Boolean = homeScore > awayScore

  def isAwayWinner: Boolean = homeScore < awayScore

  def isHomeLoser: Boolean = homeScore < awayScore

  def isAwayLoser: Boolean = homeScore > awayScore

  def showPeriods: String = periods match {
    case x: Int if x < 3 => ""
    case x: Int if x > 3 => s"${x - 2}OT"
    case _ => "OT"
  }
}

case class Quote(id: Long, quote: String, source: Option[String], url: Option[String], key: Option[String])

case class QuoteVote(id: Long, quoteId: Long, user: String, createdAt: LocalDateTime)

case class ConferenceMap(id: Long, seasonId: Long, conferenceId: Long, teamId: Long, updatedAt: LocalDateTime, updatedBy: String)

case class StatValue(id: Long, modelKey: String, statKey: String, teamID: Long, date: LocalDate, value: Double) {
  require(!modelKey.contains(":") && !modelKey.contains(" "), "Model key cannot contain ':' or ' '")
  require(!statKey.contains(":") && !statKey.contains(" "), "Stat key cannot contain ':' or ' '")
}

case class XStat(id:Long, seasonId:Long, date: LocalDate, key: String, teamId: Long, value: Option[Double], rank: Option[Int], percentile: Option[Double],mean: Option[Double], stdDev: Option[Double], min: Option[Double], max: Option[Double], n: Int)

case class LogisticModelParameter(id: Long, logisticModelName: String, featureName: String, normShift: Double, normScale: Double, coefficient: Double, fittedAsOf: LocalDate)

case class GamePrediction(id: Long, gameId: Long, modelKey: String, favoriteId: Option[Long], probability: Option[Double], spread: Option[Double], overUnder: Option[Double]) {
  def odds: Option[Double] = probability.map(x => x / (1 - x))
}

case class UserProfileData(id: Long, userID: String, key: String, value: String)

case class FavoriteLink(id: Long, userID: String, displayAs: String, link: String, order: Int, createdAt: LocalDateTime)

case class RssFeed(id: Long, name: String, url: String)

case class RssItem(id: Long, rssFeedId: Long, title: String, url: String, image: Option[String], publishTime: LocalDateTime, recordedAt: LocalDateTime)

case class Job(id: Long, name: String, description: String, cronSchedule: String, timezone: String, actorClass: Option[String], message: String, timeout: FiniteDuration, isEnabled: Boolean, updatedAt: LocalDateTime) {
  val actorPath:String = s"/user/$name"

  val quartzTriggerKey:TriggerKey =new TriggerKey(s"qz-trigger-$id",s"$name")

  val quartzJobKey:JobKey = new JobKey(s"qz-job-$id",s"$name")

  def actorSelection(context: ActorContext): ActorSelection = context.actorSelection(actorPath)
}

case class JobRun(id: Long, jobId: Long, startTime: LocalDateTime, endTime: Option[LocalDateTime], status: String, message: String) {
  require(Set("Running", "Failure", "Success").contains(status))
}

class ScheduleRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val log = Logger("schedule-repo")
  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db: JdbcBackend#DatabaseDef = dbConfig.db

  import dbConfig.profile.api._

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE_TIME),
    str => LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(str))
  )

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate] = MappedColumnType.base[LocalDate, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE),
    str => LocalDate.from(DateTimeFormatter.ISO_DATE.parse(str))
  )

  implicit val FiniteDurationMapper: BaseColumnType[FiniteDuration] = MappedColumnType.base[FiniteDuration, String](
    dur => dur.toString,
    str =>
      Duration.create(str) match {
        case d: FiniteDuration => d
        case _ => throw new RuntimeException
      }
  )

  def dumpSchema()(implicit ec: ExecutionContext): Future[(Iterable[String], Iterable[String])] = {
    Future((ddl.create.statements, ddl.drop.statements))
  }

  def createSchema(): Future[Unit] = {
    log.debug("Creating schedule schema")
    db.run(ddl.create.transactionally)
  }

  def dropSchema(): Future[Unit] = {
    log.debug("Dropping schedule schema")
    db.run(ddl.drop.transactionally)
  }

  class TeamsTable(tag: Tag) extends Table[Team](tag, "team") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def key: Rep[String] = column[String]("key", O.Length(36))

    def name: Rep[String] = column[String]("name", O.Length(64))

    def longName: Rep[String] = column[String]("long_name", O.Length(144))

    def nickname: Rep[String] = column[String]("nickname", O.Length(64))

    def optConference: Rep[String] = column[String]("opt_conference", O.Length(64))

    def logoLgUrl: Rep[Option[String]] = column[Option[String]]("logo_lg_url", O.Length(144))

    def logoSmUrl: Rep[Option[String]] = column[Option[String]]("logo_sm_url", O.Length(144))

    def primaryColor: Rep[Option[String]] = column[Option[String]]("primary_color", O.Length(64))

    def secondaryColor: Rep[Option[String]] = column[Option[String]]("secondary_color", O.Length(64))

    def officialUrl: Rep[Option[String]] = column[Option[String]]("official_url", O.Length(144))

    def officialTwitter: Rep[Option[String]] = column[Option[String]]("official_twitter", O.Length(144))

    def officialFacebook: Rep[Option[String]] = column[Option[String]]("official_facebook", O.Length(144))

    def updatedAt: Rep[LocalDateTime] = column[LocalDateTime]("updated_at")

    def updatedBy: Rep[String] = column[String]("updated_by", O.Length(64))

    def * : ProvenShape[Team] = (id, key, name, longName, nickname, optConference, logoLgUrl, logoSmUrl, primaryColor, secondaryColor, officialUrl, officialTwitter, officialFacebook, updatedAt, updatedBy) <> (Team.tupled, Team.unapply)

    def idx1: Index = index("team_idx1", key, unique = true)

    def idx2: Index = index("team_idx2", name, unique = true)
  }

  class AliasesTable(tag: Tag) extends Table[Alias](tag, "alias") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def alias: Rep[String] = column[String]("alias", O.Length(64))

    def key: Rep[String] = column[String]("key", O.Length(24))

    def * : ProvenShape[Alias] = (id, alias, key) <> (Alias.tupled, Alias.unapply)

    def idx1: Index = index("alias_idx1", alias, unique = true)

  }


  class ConferencesTable(tag: Tag) extends Table[Conference](tag, "conference") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def key: Rep[String] = column[String]("key", O.Length(48))

    def name: Rep[String] = column[String]("name", O.Length(64))

    def level: Rep[String] = column[String]("level", O.Length(16))

    def longName: Rep[String] = column[String]("long_name", O.Length(144))

    def logoLgUrl: Rep[Option[String]] = column[Option[String]]("logo_lg_url", O.Length(144))

    def logoSmUrl: Rep[Option[String]] = column[Option[String]]("logo_sm_url", O.Length(144))

    def officialUrl: Rep[Option[String]] = column[Option[String]]("official_url", O.Length(144))

    def officialTwitter: Rep[Option[String]] = column[Option[String]]("official_twitter", O.Length(64))

    def officialFacebook: Rep[Option[String]] = column[Option[String]]("official_facebook", O.Length(64))

    def lockRecord: Rep[Boolean] = column[Boolean]("lock_record")

    def updatedAt: Rep[LocalDateTime] = column[LocalDateTime]("updated_at")

    def updatedBy: Rep[String] = column[String]("updated_by", O.Length(64))

    def * : ProvenShape[Conference] = (id, key, name, level, logoLgUrl, logoSmUrl, officialUrl, officialTwitter, officialFacebook, updatedAt, updatedBy) <> (Conference.tupled, Conference.unapply)

    def idx1: Index = index("conf_idx1", key, unique = true)

    def idx2: Index = index("conf_idx2", name, unique = true)
  }


  class GamesTable(tag: Tag) extends Table[Game](tag, "game") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def seasonId: Rep[Long] = column[Long]("season_id")

    def season: ForeignKeyQuery[SeasonsTable, Season] = foreignKey("fk_game_seas", seasonId, seasons)(_.id)

    def homeTeamId: Rep[Long] = column[Long]("home_team_id")

    def homeTeam: ForeignKeyQuery[TeamsTable, Team] = foreignKey("fk_game_hteam", homeTeamId, teams)(_.id)

    def awayTeamId: Rep[Long] = column[Long]("away_team_id")

    def awayTeam: ForeignKeyQuery[TeamsTable, Team] = foreignKey("fk_game_ateam", awayTeamId, teams)(_.id)

    def date: Rep[LocalDate] = column[LocalDate]("date", O.Length(32))

    def datetime: Rep[LocalDateTime] = column[LocalDateTime]("datetime")

    def location: Rep[Option[String]] = column[Option[String]]("location", O.Length(144))

    def isNeutralSite: Rep[Boolean] = column[Boolean]("is_neutral_site")

    def tourneyKey: Rep[Option[String]] = column[Option[String]]("tourney_key", O.Length(64))

    def homeTeamSeed: Rep[Option[Int]] = column[Option[Int]]("home_team_seed")

    def awayTeamSeed: Rep[Option[Int]] = column[Option[Int]]("away_team_seed")

    def sourceKey: Rep[String] = column[String]("source_key", O.Length(32))

    def updatedAt: Rep[LocalDateTime] = column[LocalDateTime]("updated_at")

    def updatedBy: Rep[String] = column[String]("updated_by", O.Length(64))

    def * : ProvenShape[Game] = (id, seasonId, homeTeamId, awayTeamId, date, datetime, location, isNeutralSite, tourneyKey, homeTeamSeed, awayTeamSeed, sourceKey, updatedAt, updatedBy) <> (Game.tupled, Game.unapply)

  }

  class ResultsTable(tag: Tag) extends Table[Result](tag, "result") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def gameId: Rep[Long] = column[Long]("game_id")

    def game: ForeignKeyQuery[GamesTable, Game] = foreignKey("fk_res_game", gameId, games)(_.id)

    def homeScore: Rep[Int] = column[Int]("home_score")

    def awayScore: Rep[Int] = column[Int]("away_score")

    def periods: Rep[Int] = column[Int]("periods")

    def updatedAt: Rep[LocalDateTime] = column[LocalDateTime]("updated_at")

    def updatedBy: Rep[String] = column[String]("updated_by", O.Length(64))

    def * : ProvenShape[Result] = (id, gameId, homeScore, awayScore, periods, updatedAt, updatedBy) <> (Result.tupled, Result.unapply)

    def idx1: Index = index("result_idx1", gameId, unique = true)

  }

  class SeasonsTable(tag: Tag) extends Table[Season](tag, "season") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def year: Rep[Int] = column[Int]("year")

    def lock: Rep[String] = column[String]("lock", O.Length(8))

    def lockBefore: Rep[Option[LocalDate]] = column[Option[LocalDate]]("lockBefore")


    def * : ProvenShape[Season] = (id, year) <> ((Season.apply _).tupled, Season.unapply)

    def idx1: Index = index("season_idx1", year, unique = true)

  }

  class ConferenceMapsTable(tag: Tag) extends Table[ConferenceMap](tag, "conference_map") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def seasonId: Rep[Long] = column[Long]("season_id")

    def season: ForeignKeyQuery[SeasonsTable, Season] = foreignKey("fk_cm_seas", seasonId, seasons)(_.id)

    def conferenceId: Rep[Long] = column[Long]("conference_id")

    def conference: ForeignKeyQuery[ConferencesTable, Conference] = foreignKey("fk_cm_conf", conferenceId, conferences)(_.id)

    def teamId: Rep[Long] = column[Long]("team_id")

    def team: ForeignKeyQuery[TeamsTable, Team] = foreignKey("fk_cm_team", teamId, teams)(_.id)

    def lockRecord: Rep[Boolean] = column[Boolean]("lock_record")

    def updatedAt: Rep[LocalDateTime] = column[LocalDateTime]("updated_at")

    def updatedBy: Rep[String] = column[String]("updated_by", O.Length(64))

    def * : ProvenShape[ConferenceMap] = (id, seasonId, conferenceId, teamId, updatedAt, updatedBy) <> (ConferenceMap.tupled, ConferenceMap.unapply)

    def idx1: Index = index("confmap_idx1", (seasonId, conferenceId, teamId), unique = true)

  }

  class QuoteTable(tag: Tag) extends Table[Quote](tag, "quote") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def quote: Rep[String] = column[String]("quote")

    def source: Rep[Option[String]] = column[Option[String]]("source")

    def url: Rep[Option[String]] = column[Option[String]]("url")

    def key: Rep[Option[String]] = column[Option[String]]("key", O.Length(24))

    def * : ProvenShape[Quote] = (id, quote, source, url, key) <> (Quote.tupled, Quote.unapply)

  }

  class QuoteVoteTable(tag: Tag) extends Table[QuoteVote](tag, "quote_vote") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def quoteId: Rep[Long] = column[Long]("quote_id")

    def user: Rep[String] = column[String]("user")

    def createdAt: Rep[LocalDateTime] = column[LocalDateTime]("created_at")

    def * : ProvenShape[QuoteVote] = (id, quoteId, user, createdAt) <> (QuoteVote.tupled, QuoteVote.unapply)
  }

  class StatValueTable(tag: Tag) extends Table[StatValue](tag, "stat_value") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def modelKey: Rep[String] = column[String]("model_key", O.Length(32))

    def statKey: Rep[String] = column[String]("stat_key", O.Length(32))

    def teamId: Rep[Long] = column[Long]("team_id")

    def date: Rep[LocalDate] = column[LocalDate]("date", O.Length(32))

    def value: Rep[Double] = column[Double]("value")

    def * : ProvenShape[StatValue] = (id, modelKey, statKey, teamId, date, value) <> (StatValue.tupled, StatValue.unapply)

    def idx1: Index = index("stat_value_idx1", (date, modelKey, statKey, teamId), unique = true)

    def idx2: Index = index("stat_value_idx2", (date, modelKey), unique = false)

    def idx3: Index = index("stat_value_idx3", (modelKey, statKey), unique = false)

    def idx4: Index = index("stat_value_idx4", modelKey, unique = false)

  }

  //case class XStat(seasonYear: Int, date: Timestamp, statKey: String, teamKey: String, value: Double, rankAsc: Int, rankDesc: Int, percentileAsc: Double, percentileDesc: Double, mean: Double, stdDev: Double, min: Double, max: Double, n: Int)
 class XStatTable(tag: Tag) extends Table[XStat](tag, "xstat") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def seasonId: Rep[Long] = column[Long]("season_id")

    def date: Rep[LocalDate] = column[LocalDate]("date")

    def key: Rep[String] = column[String]("key", O.Length(32))

    def teamId: Rep[Long] = column[Long]("team_id", O.Length(32))


    def value: Rep[Option[Double]] = column[Option[Double]]("value")

    def rank: Rep[Option[Int]] = column[Option[Int]]("rank_asc")

    def percentile: Rep[Option[Double]] = column[Option[Double]]("pctile_asc")

    def mean: Rep[Option[Double]] = column[Option[Double]]("mean")

    def stdDev: Rep[Option[Double]] = column[Option[Double]]("std_dev")

    def min: Rep[Option[Double]] = column[Option[Double]]("min")

    def max: Rep[Option[Double]] = column[Option[Double]]("max")

    def n: Rep[Int] = column[Int]("n")


    def * : ProvenShape[XStat] = (id, seasonId, date ,key, teamId,  value, rank, percentile, mean, stdDev, min, max, n) <> (XStat.tupled, XStat.unapply)

    def idx1: Index = index("statx_value_idx1", (seasonId, date, key, teamId), unique = true)

    def idx2: Index = index("statx_value_idx2", (seasonId, date, key), unique = false)

    def idx3: Index = index("statx_value_idx3", (seasonId, key, teamId), unique = false)

    def idx4: Index = index("statx_value_idx4", (seasonId, date, teamId), unique = false)

  }

  class GamePredictionTable(tag: Tag) extends Table[GamePrediction](tag, "game_prediction") {
    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def gameId: Rep[Long] = column[Long]("game_id")

    def modelKey: Rep[String] = column[String]("model_key", O.Length(32))

    def favoriteId: Rep[Option[Long]] = column[Option[Long]]("favorite_id")

    def probability: Rep[Option[Double]] = column[Option[Double]]("probability")

    def spread: Rep[Option[Double]] = column[Option[Double]]("spread")

    def overUnder: Rep[Option[Double]] = column[Option[Double]]("over_under")

    def * : ProvenShape[GamePrediction] = (id, gameId, modelKey, favoriteId, probability, spread, overUnder) <> (GamePrediction.tupled, GamePrediction.unapply)

    def idx1: Index = index("game_pred_idx1", (gameId, modelKey), unique = true)
  }

  class LogisticModelParameterTable(tag: Tag) extends Table[LogisticModelParameter](tag, "logistic_model_parameter") {
    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def modelName: Rep[String] = column[String]("model_name", O.Length(64))

    def parameterName: Rep[String] = column[String]("parameter_name", O.Length(64))

    def normShift: Rep[Double] = column[Double]("norm_shift")

    def normScale: Rep[Double] = column[Double]("norm_scale")

    def coefficient: Rep[Double] = column[Double]("coefficient")

    def fittedAsOf: Rep[LocalDate] = column[LocalDate]("fitted_as_of", O.Length(32))

    def * : ProvenShape[LogisticModelParameter] = (id, modelName, parameterName, normShift, normScale, coefficient, fittedAsOf) <> (LogisticModelParameter.tupled, LogisticModelParameter.unapply)

    def idx1: Index = index("log_param_idx1", (modelName, parameterName, fittedAsOf), unique = true)
  }


  class UserProfileDataTable(tag: Tag) extends Table[UserProfileData](tag, "user_profile_data") {
    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def userId: Rep[String] = column[String]("user_id", O.Length(144))

    def key: Rep[String] = column[String]("key")

    def value: Rep[String] = column[String]("value")

    def * = (id, userId, key, value) <> (UserProfileData.tupled, UserProfileData.unapply)
  }


  class FavoriteLinkTable(tag: Tag) extends Table[FavoriteLink](tag, "favorite_links") {
    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def userId: Rep[String] = column[String]("user_id", O.Length(144))

    def displayAs: Rep[String] = column[String]("display_as")

    def link: Rep[String] = column[String]("link")

    def order: Rep[Int] = column[Int]("order")

    def createdAt: Rep[LocalDateTime] = column[LocalDateTime]("created_at")

    def * = (id, userId, displayAs, link, order, createdAt) <> (FavoriteLink.tupled, FavoriteLink.unapply)
  }

  class RssFeedTable(tag: Tag) extends Table[RssFeed](tag, "rss_feed") {
    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def name: Rep[String] = column[String]("name", O.Length(144))

    def url: Rep[String] = column[String]("url", O.Length(256))

    def * = (id, name, url) <> (RssFeed.tupled, RssFeed.unapply)

  }

  class RssItemTable(tag: Tag) extends Table[RssItem](tag, "rss_item") {
    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def rssFeedId: Rep[Long] = column[Long]("rss_feed_id")

    def title: Rep[String] = column[String]("title", O.Length(144))

    def url: Rep[String] = column[String]("url", O.Length(256))

    def image: Rep[Option[String]] = column[Option[String]]("image", O.Length(256))

    def publishTime: Rep[LocalDateTime] = column[LocalDateTime]("publish_time")

    def recordedAt: Rep[LocalDateTime] = column[LocalDateTime]("recorder_at")

    def * = (id, rssFeedId, title, url, image, publishTime, recordedAt) <> (RssItem.tupled, RssItem.unapply)

  }

  class JobTable(tag: Tag) extends Table[Job](tag, "job") {
    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def name: Rep[String] = column[String]("name", O.Length(144))

    def description: Rep[String] = column[String]("description", O.Length(144))

    def cronSchedule: Rep[String] = column[String]("cron_schedule", O.Length(64))

    def timezone: Rep[String] = column[String]("timezone", O.Length(64))

    def actorClass: Rep[Option[String]] = column[Option[String]]("actor_class", O.Length(256))

    def message: Rep[String] = column[String]("message", O.Length(256))

    def timeout: Rep[FiniteDuration] = column[FiniteDuration]("timeout")

    def isEnabled: Rep[Boolean] = column[Boolean]("is_enabled")

    def updatedAt: Rep[LocalDateTime] = column[LocalDateTime]("updated_at")

    def * = (id, name, description, cronSchedule, timezone, actorClass, message, timeout, isEnabled, updatedAt) <> (Job.tupled, Job.unapply)

    def idx1: Index = index("job_idx1", name, unique = true)
  }

  class JobRunTable(tag: Tag) extends Table[JobRun](tag, "job_run") {
    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def jobId: Rep[Long] = column[Long]("jobId")

    def startTime: Rep[LocalDateTime] = column[LocalDateTime]("startTime")

    def endTime: Rep[Option[LocalDateTime]] = column[Option[LocalDateTime]]("endTime")

    def status: Rep[String] = column[String]("status")

    def message: Rep[String] = column[String]("message")

    def * = (id, jobId, startTime, endTime, status, message) <> (JobRun.tupled, JobRun.unapply)
  }

  lazy val seasons: TableQuery[SeasonsTable] = TableQuery[SeasonsTable]
  lazy val games: TableQuery[GamesTable] = TableQuery[GamesTable]
  lazy val results: TableQuery[ResultsTable] = TableQuery[ResultsTable]
  lazy val teams: TableQuery[TeamsTable] = TableQuery[TeamsTable]
  lazy val aliases: TableQuery[AliasesTable] = TableQuery[AliasesTable]
  lazy val conferences: TableQuery[ConferencesTable] = TableQuery[ConferencesTable]
  lazy val conferenceMaps: TableQuery[ConferenceMapsTable] = TableQuery[ConferenceMapsTable]
  lazy val quotes: TableQuery[QuoteTable] = TableQuery[QuoteTable]
  lazy val quoteVotes: TableQuery[QuoteVoteTable] = TableQuery[QuoteVoteTable]
  lazy val statValues: TableQuery[StatValueTable] = TableQuery[StatValueTable]
  lazy val gamePredictions: TableQuery[GamePredictionTable] = TableQuery[GamePredictionTable]
  lazy val gameResults: Query[(GamesTable, Rep[Option[ResultsTable]]), (Game, Option[Result]), Seq] = games joinLeft results on (_.id === _.gameId)
  lazy val completedResults: Query[((SeasonsTable, GamesTable), ResultsTable), ((Season, Game), Result), Seq] = (seasons join games on (_.id === _.seasonId)) join results on (_._2.id === _.gameId)
  lazy val predictedResults: Query[(GamesTable, Rep[Option[GamePredictionTable]]), (Game, Option[GamePrediction]), Seq] = games joinLeft gamePredictions on (_.id === _.gameId)
  lazy val logisticModels: TableQuery[LogisticModelParameterTable] = TableQuery[LogisticModelParameterTable]
  lazy val userProfiles: TableQuery[UserProfileDataTable] = TableQuery[UserProfileDataTable]
  lazy val favoriteLinks: TableQuery[FavoriteLinkTable] = TableQuery[FavoriteLinkTable]
  lazy val rssFeeds: TableQuery[RssFeedTable] = TableQuery[RssFeedTable]
  lazy val rssItems: TableQuery[RssItemTable] = TableQuery[RssItemTable]
  lazy val jobs: TableQuery[JobTable] = TableQuery[JobTable]
  lazy val jobRuns: TableQuery[JobRunTable] = TableQuery[JobRunTable]
  lazy val xstats: TableQuery[XStatTable] = TableQuery[XStatTable]

  lazy val ddl = conferenceMaps.schema ++
    games.schema ++
    results.schema ++
    teams.schema ++
    conferences.schema ++
    seasons.schema ++
    quotes.schema ++
    aliases.schema ++
    statValues.schema ++
    gamePredictions.schema ++
    logisticModels.schema ++
    userProfiles.schema ++
    xstats.schema ++
    quoteVotes.schema ++
    favoriteLinks.schema ++
    rssFeeds.schema ++
    rssItems.schema ++
    jobs.schema ++
    jobRuns.schema
}
