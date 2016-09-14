package com.fijimf.deepfij.models

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}


case class Season(id: Long, year: Int)

case class Conference(id: Long, key: String, name: String, logoLgUrl: Option[String], logoSmUrl: Option[String], officialUrl: Option[String], officialTwitter: Option[String], officialFacebook: Option[String])

case class Game(id: Long, seasonId: Long, homeTeamId: Long, awayTeamId: Long, date: LocalDateTime, location: Option[String], tourneyKey: Option[String], homeTeamSeed: Option[Int], awayTeamSeed: Option[Int])

case class Team(id: Long, key: String, name: String, longName: String, nickname: String, logoLgUrl: Option[String], logoSmUrl: Option[String], primaryColor: Option[String], secondaryColor: Option[String], officialUrl: Option[String], officialTwitter: Option[String], officialFacebook: Option[String])

case class Result(id: Long, gameId: Long, homeScore: Int, awayScore: Int, periods: Int) {
  def margin = Math.abs(homeScore - awayScore)
}

case class ConferenceMap(id: Long, seasonId: Long, conferenceId: Long, teamId: Long)

class ScheduleRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  def dumpSchema()(implicit ec: ExecutionContext) = {
    Future((ddl.create.statements, ddl.drop.statements))
  }

  def createSchema() = {
    db.run(ddl.create.transactionally)
  }

  def dropSchema() = {
    db.run(ddl.drop.transactionally)
  }

  def all(tableQuery: TableQuery[_]) = db.run(tableQuery.to[List].result)

  def createSeason(year: Int): Future[Long] = {
    val s = Season(0, year)
    db.run(seasons returning seasons.map(_.id) += s)
  }

  def createConference(key: String, name: String,
                       logoLgUrl: Option[String] = None, logoSmUrl: Option[String] = None,
                       officialUrl: Option[String] = None, officialTwitter: Option[String] = None, officialFacebook: Option[String] = None): Future[Long] = {
    val t = Conference(0, key, name, logoLgUrl, logoSmUrl, officialUrl, officialTwitter, officialFacebook)
    db.run(conferences returning conferences.map(_.id) += t)
  }

  def createTeam(key: String, name: String, longName: String, nickname: String,
                 logoLgUrl: Option[String] = None, logoSmUrl: Option[String] = None,
                 primaryColor: Option[String] = None, secondaryColor: Option[String] = None,
                 officialUrl: Option[String] = None, officialTwitter: Option[String] = None, officialFacebook: Option[String] = None): Future[Long] = {
    val t = Team(0, key, name, longName, nickname, logoLgUrl, logoSmUrl, primaryColor, secondaryColor, officialUrl, officialTwitter, officialFacebook)
    db.run(teams returning teams.map(_.id) += t)
  }

  def mapTeam(seasonId: Long, teamId: Long, confId: Long)(implicit ec: ExecutionContext): Future[Long] = {
    val q = conferenceMaps.withFilter(cm => cm.seasonId === seasonId && cm.teamId === teamId)
    val action = q.result.headOption.flatMap((cm: Option[ConferenceMap]) => {
      cm match {
        case Some(a) => q.map(_.conferenceId).update(confId).andThen(DBIO.successful(a.id))
        case None => conferenceMaps returning conferenceMaps.map(_.id) += ConferenceMap(0L, seasonId, confId, teamId)
      }
    })
    db.run(action)
  }


  class TeamsTable(tag: Tag) extends Table[Team](tag, "TEAM") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def key = column[String]("KEY", O.Length(24))

    def name = column[String]("NAME", O.Length(64))

    def longName = column[String]("LONG_NAME", O.Length(144))

    def nickname = column[String]("NICKNAME", O.Length(64))

    def logoLgUrl = column[Option[String]]("LOGO_LG_URL", O.Length(144))

    def logoSmUrl = column[Option[String]]("LOGO_SM_URL", O.Length(144))

    def primaryColor = column[Option[String]]("PRIMARY_COLOR", O.Length(24))

    def secondaryColor = column[Option[String]]("SECONDARY_COLOR", O.Length(24))

    def officialUrl = column[Option[String]]("OFFICIAL_URL", O.Length(144))

    def officialTwitter = column[Option[String]]("OFFICIAL_TWITTER", O.Length(64))

    def officialFacebook = column[Option[String]]("OFFICIAL_FACEBOOK", O.Length(64))

    def * = (id, key, name, longName, nickname, logoLgUrl, logoSmUrl, primaryColor, secondaryColor, officialUrl, officialTwitter, officialFacebook) <>(Team.tupled, Team.unapply)

    def idx1 = index("team_idx1", key, unique = true)

    def idx2 = index("team_idx2", name, unique = true)
  }


  class ConferencesTable(tag: Tag) extends Table[Conference](tag, "CONFERENCE") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def key = column[String]("KEY", O.Length(24))

    def name = column[String]("NAME", O.Length(64))

    def longName = column[String]("LONG_NAME", O.Length(144))

    def logoLgUrl = column[Option[String]]("LOGO_LG_URL", O.Length(144))

    def logoSmUrl = column[Option[String]]("LOGO_SM_URL", O.Length(144))

    def officialUrl = column[Option[String]]("OFFICIAL_URL", O.Length(144))

    def officialTwitter = column[Option[String]]("OFFICIAL_TWITTER", O.Length(64))

    def officialFacebook = column[Option[String]]("OFFICIAL_FACEBOOK", O.Length(64))

    def * = (id, key, name, logoLgUrl, logoSmUrl, officialUrl, officialTwitter, officialFacebook) <>(Conference.tupled, Conference.unapply)

    def idx1 = index("conf_idx1", key, unique = true)

    def idx2 = index("conf_idx2", name, unique = true)
  }


  class GamesTable(tag: Tag) extends Table[Game](tag, "GAME") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def seasonId = column[Long]("SEASON_ID")

    def homeTeamId = column[Long]("HOME_TEAM_ID")

    def awayTeamId = column[Long]("AWAY_TEAM_ID")

    def date = column[LocalDateTime]("DATE")

    def location = column[Option[String]]("LOCATION", O.Length(144))

    def tourneyKey = column[Option[String]]("TOURNEY_KEY", O.Length(64))

    def homeTeamSeed = column[Option[Int]]("HOME_TEAM_SEED")

    def awayTeamSeed = column[Option[Int]]("AWAY_TEAM_SEED")

    def * = (id, seasonId, homeTeamId, awayTeamId, date, location, tourneyKey, homeTeamSeed, awayTeamSeed) <>(Game.tupled, Game.unapply)

  }

  class ResultsTable(tag: Tag) extends Table[Result](tag, "RESULT") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def gameId = column[Long]("GAME_ID")

    def homeScore = column[Int]("HOME_SCORE")

    def awayScore = column[Int]("AWAY_SCORE")

    def periods = column[Int]("PERIODS")

    def * = (id, gameId, homeScore, awayScore, periods) <>(Result.tupled, Result.unapply)

    def idx1 = index("result_idx1", gameId, unique = true)

  }

  class SeasonsTable(tag: Tag) extends Table[Season](tag, "SEASON") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def year = column[Int]("YEAR")


    def * = (id, year) <>(Season.tupled, Season.unapply)

    def idx1 = index("season_idx1", year, unique = true)

  }

  class ConferenceMapsTable(tag: Tag) extends Table[ConferenceMap](tag, "CONFERENCE_MAP") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def seasonId = column[Long]("SEASON_ID")

    def conferenceId = column[Long]("CONFERENCE_ID")

    def teamId = column[Long]("TEAM_ID")

    def * = (id, seasonId, conferenceId, teamId) <>(ConferenceMap.tupled, ConferenceMap.unapply)

    def idx1 = index("confmap_idx1", (seasonId, conferenceId, teamId), unique = true)

  }


  implicit val JavaLocalDateTimeMapper = MappedColumnType.base[LocalDateTime, Long](
    ldt => ldt.toEpochSecond(ZoneOffset.UTC),
    long => LocalDateTime.ofEpochSecond(long, 0, ZoneOffset.UTC)
  )

  lazy val seasons = TableQuery[SeasonsTable]
  lazy val games = TableQuery[GamesTable]
  lazy val results = TableQuery[ResultsTable]
  lazy val teams = TableQuery[TeamsTable]
  lazy val conferences = TableQuery[ConferencesTable]
  lazy val conferenceMaps = TableQuery[ConferenceMapsTable]

  lazy val ddl = conferenceMaps.schema ++ games.schema ++ results.schema ++ teams.schema ++ conferences.schema ++  seasons.schema
}
