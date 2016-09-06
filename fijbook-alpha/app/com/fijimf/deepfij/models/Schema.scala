package com.fijimf.deepfij.models

import java.time.{ZoneOffset, LocalDateTime}
import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.Future

case class Conference(id: Long, key: String, name: String, logoLgUrl: Option[String], logoSmUrl: Option[String], officialUrl: Option[String], officialTwitter: Option[String], officialFacebook: Option[String])

case class Game(id: Long, seasonId: Long, homeTeamId: Long, awayTeamId: Long, date: LocalDateTime, location: Option[String], tourneyKey: Option[String], homeTeamSeed: Option[Int], awayTeamSeed: Option[Int])

case class Team(id: Long, key: String, name: String, longName: String, nickname: String, logoLgUrl: Option[String], logoSmUrl: Option[String], primaryColor: Option[String], secondaryColor: Option[String], officialUrl: Option[String], officialTwitter: Option[String], officialFacebook: Option[String])

case class Result(id: Long, gameId: Long, homeScore: Int, awayScore: Int, periods: Int) {
  def margin = Math.abs(homeScore - awayScore)
}

case class ConferenceMap(id: Long, seasonId: Long, conferenceId: Long, teamId: Long)

class Repo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  def all(tableQuery: TableQuery[_]) = db.run(tableQuery.to[List].result)

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


   class TeamsTable(tag: Tag) extends Table[Team](tag, "TEAM") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def key = column[String]("KEY")

    def name = column[String]("NAME")

    def longName = column[String]("LONG_NAME")

    def nickname = column[String]("NICKNAME")

    def logoLgUrl = column[Option[String]]("LOGO_LG_URL")

    def logoSmUrl = column[Option[String]]("LOGO_SM_URL")

    def primaryColor = column[Option[String]]("PRIMARY_COLOR")

    def secondaryColor = column[Option[String]]("SECONDARY_COLOR")

    def officialUrl = column[Option[String]]("OFFICIAL_URL")

    def officialTwitter = column[Option[String]]("OFFICIAL_TWITTER")

    def officialFacebook = column[Option[String]]("OFFICIAL_FACEBOOK")

    def * = (id, key, name, longName, nickname, logoLgUrl, logoSmUrl, primaryColor, secondaryColor, officialUrl, officialTwitter, officialFacebook) <>(Team.tupled, Team.unapply)

  }


   class ConferencesTable(tag: Tag) extends Table[Conference](tag, "CONFERENCE") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def key = column[String]("KEY")

    def name = column[String]("NAME")

    def longName = column[String]("LONG_NAME")

    def logoLgUrl = column[Option[String]]("LOGO_LG_URL")

    def logoSmUrl = column[Option[String]]("LOGO_SM_URL")

    def officialUrl = column[Option[String]]("OFFICIAL_URL")

    def officialTwitter = column[Option[String]]("OFFICIAL_TWITTER")

    def officialFacebook = column[Option[String]]("OFFICIAL_FACEBOOK")

    def * = (id, key, name, logoLgUrl, logoSmUrl, officialUrl, officialTwitter, officialFacebook) <>(Conference.tupled, Conference.unapply)

  }


   class GamesTable(tag: Tag) extends Table[Game](tag, "GAME") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def seasonId = column[Long]("SEASON_ID")

    def homeTeamId = column[Long]("HOME_TEAM_ID")

    def awayTeamId = column[Long]("AWAY_TEAM_ID")

    def date = column[LocalDateTime]("DATE")

    def location = column[Option[String]]("LOCATION")

    def tourneyKey = column[Option[String]]("TOURNEY_KEY")

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

  }

   class ConferenceMapsTable(tag: Tag) extends Table[ConferenceMap](tag, "CONFERENCE_MAP") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def seasonId = column[Long]("SEASON_ID")

    def conferenceId = column[Long]("CONFERENCE_ID")

    def teamId = column[Long]("TEAM_ID")

    def * = (id, seasonId, conferenceId, teamId) <>(ConferenceMap.tupled, ConferenceMap.unapply)

  }


  implicit val JavaLocalDateTimeMapper = MappedColumnType.base[LocalDateTime, Long](
    ldt => ldt.toEpochSecond(ZoneOffset.UTC),
    long => LocalDateTime.ofEpochSecond(long, 0, ZoneOffset.UTC)
  )

  lazy val games = TableQuery[GamesTable]
  lazy val results = TableQuery[ResultsTable]
  lazy val teams = TableQuery[TeamsTable]
  lazy val conferences = TableQuery[ConferencesTable]
  lazy val conferenceMaps = TableQuery[ConferenceMapsTable]
}


//def all: Future[List[Conference]] = db.run(Conferences.to[List].result)
//
//def findById(id: Long): Future[Conference] = db.run(Conferences.filter(_.id === id).result.head)