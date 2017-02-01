package com.fijimf.deepfij.models

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import javax.inject.Inject

import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend
import slick.lifted._

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
  def canUpdate(d: LocalDate): Boolean = d.isAfter(date)
}

case class Season(id: Long, year: Int, lock: String, lockBefore: Option[LocalDate]) {
  val status: SeasonLockStatus = lock.toLowerCase match {
    case "lock" => Locked
    case "update" => Updating
    case _ =>
      lockBefore match {
        case Some(date) => LockedBefore(date)
        case None => Open
      }
  }
  val startDate: LocalDate = LocalDate.of(year - 1, 11, 1)
  val endDate: LocalDate = LocalDate.of(year, 5, 1)
  val dates: List[LocalDate] = Iterator.iterate(startDate) {
    _.plusDays(1)
  }.takeWhile(_.isBefore(endDate)).toList
}

case class Conference(id: Long, key: String, name: String, logoLgUrl: Option[String], logoSmUrl: Option[String], officialUrl: Option[String], officialTwitter: Option[String], officialFacebook: Option[String], lockRecord: Boolean, updatedAt: LocalDateTime, updatedBy: String)

case class Game(id: Long, seasonId: Long, homeTeamId: Long, awayTeamId: Long, date: LocalDate, datetime: LocalDateTime, location: Option[String], isNeutralSite: Boolean, tourneyKey: Option[String], homeTeamSeed: Option[Int], awayTeamSeed: Option[Int], lockRecord: Boolean, updatedAt: LocalDateTime, updatedBy: String) {
}

case class Team(id: Long, key: String, name: String, longName: String, nickname: String, optConference: String, logoLgUrl: Option[String], logoSmUrl: Option[String], primaryColor: Option[String], secondaryColor: Option[String], officialUrl: Option[String], officialTwitter: Option[String], officialFacebook: Option[String], lockRecord: Boolean, updatedAt: LocalDateTime, updatedBy: String) extends Ordering[Team] {
  override def compare(x: Team, y: Team): Int = x.name.compare(y.name)
}

case class Alias(id: Long, alias: String, key: String)

case class Result(id: Long, gameId: Long, homeScore: Int, awayScore: Int, periods: Int, lockRecord: Boolean, updatedAt: LocalDateTime, updatedBy: String) {
  def margin: Int = Math.abs(homeScore - awayScore)

  def isHomeWinner: Boolean = homeScore > awayScore

  def isAwayWinner: Boolean = homeScore < awayScore

  def isHomeLoser: Boolean = homeScore < awayScore

  def isAwayLoser: Boolean = homeScore > awayScore
}

case class Quote(id: Long, quote: String, source: Option[String], url: Option[String], key:Option[String])

case class ConferenceMap(id: Long, seasonId: Long, conferenceId: Long, teamId: Long, lockRecord: Boolean, updatedAt: LocalDateTime, updatedBy: String)

case class StatValue(id: Long, modelKey: String, statKey: String, teamID: Long, date: LocalDate, value: Double)

object StatUtil {
  def transformSnapshot(svs: List[StatValue], map: (StatValue) => Team, higherIsBetter: Boolean): List[(Int, StatValue, Team)] = {
    svs.map(sv => map(sv) -> sv).sortBy(tup => if (higherIsBetter) -tup._2.value else tup._2.value).foldLeft(List.empty[(Int, StatValue, Team)])((accum: List[(Int, StatValue, Team)], response: (Team, StatValue)) => {
      val rank = accum match {
        case Nil => 1
        case head :: tail => if (head._2.value == response._2.value) head._1 else accum.size + 1
      }
      (rank, response._2, response._1) :: accum
    }).reverse
  }
}

case class GamePrediction(id: Long, gameId: Long, modelKey:String, favoriteId:Option[Long], probability:Option[Double], spread:Option[Double], overUnder:Option[Double] )

class ScheduleRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val log = Logger("schedule-repo")
  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db: JdbcBackend#DatabaseDef = dbConfig.db

  import dbConfig.driver.api._

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE_TIME),
    str => LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(str))
  )

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate] = MappedColumnType.base[LocalDate, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE),
    str => LocalDate.from(DateTimeFormatter.ISO_DATE.parse(str))
  )

  def dumpSchema()(implicit ec: ExecutionContext): Future[(Iterable[String], Iterable[String])] = {
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

  def all(tableQuery: TableQuery[_]): Future[List[_]] = db.run(tableQuery.to[List].result)

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

  def createTeam(key: String, name: String, longName: String, nickname: String, optConference: String,
                 logoLgUrl: Option[String] = None, logoSmUrl: Option[String] = None,
                 primaryColor: Option[String] = None, secondaryColor: Option[String] = None,
                 officialUrl: Option[String] = None, officialTwitter: Option[String] = None, officialFacebook: Option[String] = None, updatedBy: String): Future[Long] = {
    val t = Team(0, key, name, longName, nickname, optConference, logoLgUrl, logoSmUrl, primaryColor, secondaryColor, officialUrl, officialTwitter, officialFacebook, false, LocalDateTime.now(), updatedBy)
    db.run(teams returning teams.map(_.id) += t)
  }

  def getTeams(implicit ec: ExecutionContext): Future[Map[String, Team]] = db.run(teams.to[List].map(t => t.key -> t).result).map(_.toMap)


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

    def lockRecord: Rep[Boolean] = column[Boolean]("lock_record")

    def updatedAt: Rep[LocalDateTime] = column[LocalDateTime]("updated_at")

    def updatedBy: Rep[String] = column[String]("updated_by", O.Length(64))

    def * : ProvenShape[Team] = (id, key, name, longName, nickname, optConference, logoLgUrl, logoSmUrl, primaryColor, secondaryColor, officialUrl, officialTwitter, officialFacebook, lockRecord, updatedAt, updatedBy) <> (Team.tupled, Team.unapply)

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

    def longName: Rep[String] = column[String]("long_name", O.Length(144))

    def logoLgUrl: Rep[Option[String]] = column[Option[String]]("logo_lg_url", O.Length(144))

    def logoSmUrl: Rep[Option[String]] = column[Option[String]]("logo_sm_url", O.Length(144))

    def officialUrl: Rep[Option[String]] = column[Option[String]]("official_url", O.Length(144))

    def officialTwitter: Rep[Option[String]] = column[Option[String]]("official_twitter", O.Length(64))

    def officialFacebook: Rep[Option[String]] = column[Option[String]]("official_facebook", O.Length(64))

    def lockRecord: Rep[Boolean] = column[Boolean]("lock_record")

    def updatedAt: Rep[LocalDateTime] = column[LocalDateTime]("updated_at")

    def updatedBy: Rep[String] = column[String]("updated_by", O.Length(64))

    def * : ProvenShape[Conference] = (id, key, name, logoLgUrl, logoSmUrl, officialUrl, officialTwitter, officialFacebook, lockRecord, updatedAt, updatedBy) <> (Conference.tupled, Conference.unapply)

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

    def date: Rep[LocalDate] = column[LocalDate]("date")

    def datetime: Rep[LocalDateTime] = column[LocalDateTime]("datetime")

    def location: Rep[Option[String]] = column[Option[String]]("location", O.Length(144))

    def isNeutralSite: Rep[Boolean] = column[Boolean]("is_neutral_site")

    def tourneyKey: Rep[Option[String]] = column[Option[String]]("tourney_key", O.Length(64))

    def homeTeamSeed: Rep[Option[Int]] = column[Option[Int]]("home_team_seed")

    def awayTeamSeed: Rep[Option[Int]] = column[Option[Int]]("away_team_seed")

    def lockRecord: Rep[Boolean] = column[Boolean]("lock_record")

    def updatedAt: Rep[LocalDateTime] = column[LocalDateTime]("updated_at")

    def updatedBy: Rep[String] = column[String]("updated_by", O.Length(64))

    def * : ProvenShape[Game] = (id, seasonId, homeTeamId, awayTeamId, date, datetime, location, isNeutralSite, tourneyKey, homeTeamSeed, awayTeamSeed, lockRecord, updatedAt, updatedBy) <> (Game.tupled, Game.unapply)

  }

  class ResultsTable(tag: Tag) extends Table[Result](tag, "result") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def gameId: Rep[Long] = column[Long]("game_id")

    def game: ForeignKeyQuery[GamesTable, Game] = foreignKey("fk_res_game", gameId, games)(_.id)

    def homeScore: Rep[Int] = column[Int]("home_score")

    def awayScore: Rep[Int] = column[Int]("away_score")

    def periods: Rep[Int] = column[Int]("periods")

    def lockRecord: Rep[Boolean] = column[Boolean]("lock_record")

    def updatedAt: Rep[LocalDateTime] = column[LocalDateTime]("updated_at")

    def updatedBy: Rep[String] = column[String]("updated_by", O.Length(64))

    def * : ProvenShape[Result] = (id, gameId, homeScore, awayScore, periods, lockRecord, updatedAt, updatedBy) <> (Result.tupled, Result.unapply)

    def idx1: Index = index("result_idx1", gameId, unique = true)

  }

  class SeasonsTable(tag: Tag) extends Table[Season](tag, "season") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def year: Rep[Int] = column[Int]("year")

    def lock: Rep[String] = column[String]("lock", O.Length(8))

    def lockBefore: Rep[Option[LocalDate]] = column[Option[LocalDate]]("lockBefore")


    def * : ProvenShape[Season] = (id, year, lock, lockBefore) <> (Season.tupled, Season.unapply)

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

    def * : ProvenShape[ConferenceMap] = (id, seasonId, conferenceId, teamId, lockRecord, updatedAt, updatedBy) <> (ConferenceMap.tupled, ConferenceMap.unapply)

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

  class StatValueTable(tag: Tag) extends Table[StatValue](tag, "stat_value") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def modelKey: Rep[String] = column[String]("model_key", O.Length(32))

    def statKey: Rep[String] = column[String]("stat_key", O.Length(32))

    def teamId: Rep[Long] = column[Long]("team_id")

    def date: Rep[LocalDate] = column[LocalDate]("date")

    def value: Rep[Double] = column[Double]("value")

    def * : ProvenShape[StatValue] = (id, modelKey, statKey, teamId, date, value) <> (StatValue.tupled, StatValue.unapply)

    def idx1: Index = index("stat_value_idx1", (date, modelKey, statKey, teamId), unique = true)
    def idx2: Index = index("stat_value_idx2", (date, modelKey), unique = false)
    def idx3: Index = index("stat_value_idx3", (modelKey, statKey), unique = false)
    def idx4: Index = index("stat_value_idx4", (modelKey), unique = false)

  }

  class GamePredictionTable(tag:Tag) extends Table[GamePrediction](tag,"game_prediction"){
    def id: Rep[Long]=column[Long]("id", O.AutoInc, O.PrimaryKey)
    def gameId: Rep[Long]=column[Long]("game_id")
    def modelKey:Rep[String]=column[String]("model_key", O.Length(32))
    def favoriteId:Rep[Option[Long]] = column[Option[Long]]("favorite_id")
    def probability:Rep[Option[Double]]=column[Option[Double]]("probability")
    def spread:Rep[Option[Double]] = column[Option[Double]]("spread")
    def overUnder:Rep[Option[Double]] = column[Option[Double]]("over_under")
    def * : ProvenShape[GamePrediction] = (id, gameId, modelKey, favoriteId, probability, spread, overUnder) <> (GamePrediction.tupled, GamePrediction.unapply)
    def idx1: Index = index("game_pred_idx1", (gameId, modelKey), unique = true)
  }


  lazy val seasons: TableQuery[SeasonsTable] = TableQuery[SeasonsTable]
  lazy val games: TableQuery[GamesTable] = TableQuery[GamesTable]
  lazy val results: TableQuery[ResultsTable] = TableQuery[ResultsTable]
  lazy val teams: TableQuery[TeamsTable] = TableQuery[TeamsTable]
  lazy val aliases: TableQuery[AliasesTable] = TableQuery[AliasesTable]
  lazy val conferences: TableQuery[ConferencesTable] = TableQuery[ConferencesTable]
  lazy val conferenceMaps: TableQuery[ConferenceMapsTable] = TableQuery[ConferenceMapsTable]
  lazy val quotes: TableQuery[QuoteTable] = TableQuery[QuoteTable]
  lazy val statValues: TableQuery[StatValueTable] = TableQuery[StatValueTable]
  lazy val gamePredictions: TableQuery[GamePredictionTable] = TableQuery[GamePredictionTable]
  lazy val gameResults: Query[(GamesTable, Rep[Option[ResultsTable]]), (Game, Option[Result]), Seq] = games joinLeft results on (_.id === _.gameId)
  lazy val predictedResults: Query[(GamesTable, Rep[Option[GamePredictionTable]]), (Game, Option[GamePrediction]), Seq] = games joinLeft gamePredictions on (_.id === _.gameId)

  lazy val ddl = conferenceMaps.schema ++ games.schema ++ results.schema ++ teams.schema ++ conferences.schema ++ seasons.schema ++ quotes.schema ++ aliases.schema ++ statValues.schema ++ gamePredictions.schema
}
