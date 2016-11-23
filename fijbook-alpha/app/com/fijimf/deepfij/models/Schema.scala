package com.fijimf.deepfij.models

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import javax.inject.Inject

import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

sealed trait SeasonLockStatus {
  def canUpdate(date: LocalDate): Boolean
}

case object Locked extends SeasonLockStatus {
  def canUpdate(d: LocalDate) = false
}

case object Open extends SeasonLockStatus {
  def canUpdate(d: LocalDate) = true
}

case object Updating extends SeasonLockStatus {
  def canUpdate(d: LocalDate) = true
}

case class LockedBefore(date: LocalDate) extends SeasonLockStatus {
  def canUpdate(d: LocalDate) = d.isAfter(date)
}

case class Season(id: Long, year: Int, lock: String, lockBefore: Option[LocalDate]) {
  val status:SeasonLockStatus = lock.toLowerCase match {
    case "lock" => Locked
    case "update" => Updating
    case _ =>
      lockBefore match {
        case Some(date) => LockedBefore(date)
        case None => Open
      }
  }
  val startDate: LocalDate = LocalDate.of(year-1, 11, 1)
  val endDate: LocalDate = LocalDate.of(year, 5, 1)
  val dates: List[LocalDate] = Iterator.iterate(startDate) {
    _.plusDays(1)
  }.takeWhile(_.isBefore(endDate)).toList
}

case class Conference(id: Long, key: String, name: String, logoLgUrl: Option[String], logoSmUrl: Option[String], officialUrl: Option[String], officialTwitter: Option[String], officialFacebook: Option[String], lockRecord: Boolean, updatedAt: LocalDateTime, updatedBy: String)

case class Game(id: Long, seasonId: Long, homeTeamId: Long, awayTeamId: Long, date:LocalDate, datetime: LocalDateTime, location: Option[String], tourneyKey: Option[String], homeTeamSeed: Option[Int], awayTeamSeed: Option[Int], lockRecord: Boolean, updatedAt: LocalDateTime, updatedBy: String)

case class Team(id: Long, key: String, name: String, longName: String, nickname: String, logoLgUrl: Option[String], logoSmUrl: Option[String], primaryColor: Option[String], secondaryColor: Option[String], officialUrl: Option[String], officialTwitter: Option[String], officialFacebook: Option[String], lockRecord: Boolean, updatedAt: LocalDateTime, updatedBy: String)

case class Alias(id: Long, alias: String, key: String)

case class Result(id: Long, gameId: Long, homeScore: Int, awayScore: Int, periods: Int, lockRecord: Boolean, updatedAt: LocalDateTime, updatedBy: String) {
  def margin = Math.abs(homeScore - awayScore)
}

case class Quote(id: Long, quote: String, source: Option[String], url: Option[String])

case class ConferenceMap(id: Long, seasonId: Long, conferenceId: Long, teamId: Long, lockRecord: Boolean, updatedAt: LocalDateTime, updatedBy: String)

class ScheduleRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val log = Logger("schedule-repo")
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  implicit val JavaLocalDateTimeMapper = MappedColumnType.base[LocalDateTime, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE_TIME),
    str => LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(str))
  )

  implicit val JavaLocalDateMapper = MappedColumnType.base[LocalDate, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE),
    str => LocalDate.from(DateTimeFormatter.ISO_DATE.parse(str))
  )

  def dumpSchema()(implicit ec: ExecutionContext) = {
    Future((ddl.create.statements, ddl.drop.statements))
  }

  def createSchema() = {
    log.warn("Creating schedule schema")
    db.run(ddl.create.transactionally)
  }

  def dropSchema() = {
    log.warn("Dropping schedule schema")
    db.run(ddl.drop.transactionally)
  }

  def all(tableQuery: TableQuery[_]) = db.run(tableQuery.to[List].result)

  def createSeason(year: Int): Future[Long] = {
    val s = Season(0, year, "open", None)
    db.run(seasons returning seasons.map(_.id) += s)
  }

  def createConference(key: String, name: String,
                       logoLgUrl: Option[String] = None, logoSmUrl: Option[String] = None,
                       officialUrl: Option[String] = None, officialTwitter: Option[String] = None, officialFacebook: Option[String] = None, updatedBy: String): Future[Long] = {
    val t = Conference(0, key, name, logoLgUrl, logoSmUrl, officialUrl, officialTwitter, officialFacebook, false, LocalDateTime.now(), updatedBy)
    db.run(conferences returning conferences.map(_.id) += t)
  }

  def createTeam(key: String, name: String, longName: String, nickname: String,
                 logoLgUrl: Option[String] = None, logoSmUrl: Option[String] = None,
                 primaryColor: Option[String] = None, secondaryColor: Option[String] = None,
                 officialUrl: Option[String] = None, officialTwitter: Option[String] = None, officialFacebook: Option[String] = None, updatedBy: String): Future[Long] = {
    val t = Team(0, key, name, longName, nickname, logoLgUrl, logoSmUrl, primaryColor, secondaryColor, officialUrl, officialTwitter, officialFacebook, false, LocalDateTime.now(), updatedBy)
    db.run(teams returning teams.map(_.id) += t)
  }

  def getTeams(implicit ec: ExecutionContext) = db.run(teams.to[List].map(t => t.key -> t).result).map(_.toMap)


  def mapTeam(seasonId: Long, teamId: Long, confId: Long, updatedBy: String)(implicit ec: ExecutionContext): Future[Long] = {
    val q = conferenceMaps.withFilter(cm => cm.seasonId === seasonId && cm.teamId === teamId)
    val action = q.result.headOption.flatMap((cm: Option[ConferenceMap]) => {
      cm match {
        case Some(a) => q.map(_.conferenceId).update(confId).andThen(DBIO.successful(a.id))
        case None => conferenceMaps returning conferenceMaps.map(_.id) += ConferenceMap(0L, seasonId, confId, teamId, false, LocalDateTime.now(), updatedBy)
      }
    })
    db.run(action)
  }


  class TeamsTable(tag: Tag) extends Table[Team](tag, "team") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def key = column[String]("key", O.Length(36))

    def name = column[String]("name", O.Length(64))

    def longName = column[String]("long_name", O.Length(144))

    def nickname = column[String]("nickname", O.Length(64))

    def logoLgUrl = column[Option[String]]("logo_lg_url", O.Length(144))

    def logoSmUrl = column[Option[String]]("logo_sm_url", O.Length(144))

    def primaryColor = column[Option[String]]("primary_color", O.Length(64))

    def secondaryColor = column[Option[String]]("secondary_color", O.Length(64))

    def officialUrl = column[Option[String]]("official_url", O.Length(144))

    def officialTwitter = column[Option[String]]("official_twitter", O.Length(144))

    def officialFacebook = column[Option[String]]("official_facebook", O.Length(144))

    def lockRecord = column[Boolean]("lock_record")

    def updatedAt = column[LocalDateTime]("updated_at")

    def updatedBy = column[String]("updated_by", O.Length(64))

    def * = (id, key, name, longName, nickname, logoLgUrl, logoSmUrl, primaryColor, secondaryColor, officialUrl, officialTwitter, officialFacebook, lockRecord, updatedAt, updatedBy) <> (Team.tupled, Team.unapply)

    def idx1 = index("team_idx1", key, unique = true)

    def idx2 = index("team_idx2", name, unique = true)
  }

  class AliasesTable(tag: Tag) extends Table[Alias](tag, "alias") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def alias = column[String]("alias", O.Length(64))

    def key = column[String]("key", O.Length(24))

    def * = (id, alias, key) <> (Alias.tupled, Alias.unapply)

    def idx1 = index("alias_idx1", alias, unique = true)

  }


  class ConferencesTable(tag: Tag) extends Table[Conference](tag, "conference") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def key = column[String]("key", O.Length(24))

    def name = column[String]("name", O.Length(64))

    def longName = column[String]("long_name", O.Length(144))

    def logoLgUrl = column[Option[String]]("logo_lg_url", O.Length(144))

    def logoSmUrl = column[Option[String]]("logo_sm_url", O.Length(144))

    def officialUrl = column[Option[String]]("official_url", O.Length(144))

    def officialTwitter = column[Option[String]]("official_twitter", O.Length(64))

    def officialFacebook = column[Option[String]]("official_facebook", O.Length(64))

    def lockRecord = column[Boolean]("lock_record")

    def updatedAt = column[LocalDateTime]("updated_at")

    def updatedBy = column[String]("updated_by", O.Length(64))

    def * = (id, key, name, logoLgUrl, logoSmUrl, officialUrl, officialTwitter, officialFacebook, lockRecord, updatedAt, updatedBy) <> (Conference.tupled, Conference.unapply)

    def idx1 = index("conf_idx1", key, unique = true)

    def idx2 = index("conf_idx2", name, unique = true)
  }


  class GamesTable(tag: Tag) extends Table[Game](tag, "game") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def seasonId = column[Long]("season_id")

    def season = foreignKey("fk_game_seas", seasonId, seasons)(_.id)

    def homeTeamId = column[Long]("home_team_id")

    def homeTeam = foreignKey("fk_game_hteam", homeTeamId, teams)(_.id)

    def awayTeamId = column[Long]("away_team_id")

    def awayTeam = foreignKey("fk_game_ateam", awayTeamId, teams)(_.id)

    def date = column[LocalDate]("date")

    def datetime = column[LocalDateTime]("datetime")

    def location = column[Option[String]]("location", O.Length(144))

    def tourneyKey = column[Option[String]]("tourney_key", O.Length(64))

    def homeTeamSeed = column[Option[Int]]("home_team_seed")

    def awayTeamSeed = column[Option[Int]]("away_team_seed")

    def lockRecord = column[Boolean]("lock_record")

    def updatedAt = column[LocalDateTime]("updated_at")

    def updatedBy = column[String]("updated_by", O.Length(64))

    def * = (id, seasonId, homeTeamId, awayTeamId, date, datetime, location, tourneyKey, homeTeamSeed, awayTeamSeed, lockRecord, updatedAt, updatedBy) <> (Game.tupled, Game.unapply)

  }

  class ResultsTable(tag: Tag) extends Table[Result](tag, "result") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def gameId = column[Long]("game_id")

    def game = foreignKey("fk_res_game", gameId, games)(_.id)

    def homeScore = column[Int]("home_score")

    def awayScore = column[Int]("away_score")

    def periods = column[Int]("periods")

    def lockRecord = column[Boolean]("lock_record")

    def updatedAt = column[LocalDateTime]("updated_at")

    def updatedBy = column[String]("updated_by", O.Length(64))

    def * = (id, gameId, homeScore, awayScore, periods, lockRecord, updatedAt, updatedBy) <> (Result.tupled, Result.unapply)

    def idx1 = index("result_idx1", gameId, unique = true)

  }

  class SeasonsTable(tag: Tag) extends Table[Season](tag, "season") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def year = column[Int]("year")

    def lock = column[String]("lock", O.Length(8))

    def lockBefore = column[Option[LocalDate]]("lockBefore")


    def * = (id, year, lock, lockBefore) <> (Season.tupled, Season.unapply)

    def idx1 = index("season_idx1", year, unique = true)

  }

  class ConferenceMapsTable(tag: Tag) extends Table[ConferenceMap](tag, "conference_map") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def seasonId = column[Long]("season_id")

    def season = foreignKey("fk_cm_seas", seasonId, seasons)(_.id)

    def conferenceId = column[Long]("conference_id")

    def conference = foreignKey("fk_cm_conf", conferenceId, conferences)(_.id)

    def teamId = column[Long]("team_id")

    def team = foreignKey("fk_cm_team", teamId, teams)(_.id)

    def lockRecord = column[Boolean]("lock_record")

    def updatedAt = column[LocalDateTime]("updated_at")

    def updatedBy = column[String]("updated_by", O.Length(64))

    def * = (id, seasonId, conferenceId, teamId, lockRecord, updatedAt, updatedBy) <> (ConferenceMap.tupled, ConferenceMap.unapply)

    def idx1 = index("confmap_idx1", (seasonId, conferenceId, teamId), unique = true)

  }

  class QuoteTable(tag: Tag) extends Table[Quote](tag, "quote") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def quote = column[String]("quote")

    def source = column[Option[String]]("source")

    def url = column[Option[String]]("url")

    def * = (id, quote, source, url) <> (Quote.tupled, Quote.unapply)

  }

  lazy val seasons = TableQuery[SeasonsTable]
  lazy val games = TableQuery[GamesTable]
  lazy val results = TableQuery[ResultsTable]
  lazy val teams = TableQuery[TeamsTable]
  lazy val aliases = TableQuery[AliasesTable]
  lazy val conferences = TableQuery[ConferencesTable]
  lazy val conferenceMaps = TableQuery[ConferenceMapsTable]
  lazy val quotes = TableQuery[QuoteTable]

  lazy val gameResults = games joinLeft results on (_.id === _.gameId)

  lazy val ddl = conferenceMaps.schema ++ games.schema ++ results.schema ++ teams.schema ++ conferences.schema ++ seasons.schema ++ quotes.schema ++ aliases.schema
}
