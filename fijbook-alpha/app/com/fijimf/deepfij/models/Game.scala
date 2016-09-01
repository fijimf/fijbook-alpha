package com.fijimf.deepfij.models

import java.time.{ZoneOffset, LocalDateTime}
import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

case class Game(id: Long, homeTeamId: Long, awayTeamId: Long, date: LocalDateTime, location: Option[String], tourneyKey: Option[String], homeTeamSeed: Option[Int], awayTeamSeed: Option[Int])

class GameRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  private val Games = TableQuery[GamesTable]

  private class GamesTable(tag: Tag) extends Table[Game](tag, "GAME") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def homeTeamId = column[Long]("HOME_TEAM_ID")

    def awayTeamId = column[Long]("AWAY_TEAM_ID")

    def date = column[LocalDateTime]("DATE")

    def location = column[Option[String]]("LOCATION")

    def tourneyKey = column[Option[String]]("TOURNEY_KEY")

    def homeTeamSeed = column[Option[Int]]("HOME_TEAM_SEED")

    def awayTeamSeed = column[Option[Int]]("AWAY_TEAM_SEED")

    def * = (id, homeTeamId, awayTeamId, date, location, tourneyKey, homeTeamSeed, awayTeamSeed) <>(Game.tupled, Game.unapply)

  }

  implicit val JavaLocalDateTimeMapper = MappedColumnType.base[LocalDateTime, Long](
    ldt => ldt.toEpochSecond(ZoneOffset.UTC),
    long => LocalDateTime.ofEpochSecond(long, 0, ZoneOffset.UTC)
  )
}
