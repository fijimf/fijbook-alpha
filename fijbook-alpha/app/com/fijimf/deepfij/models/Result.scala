package com.fijimf.deepfij.models

import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

case class Result(id: Long, gameId: Long, homeScore: Int, awayScore: Int, periods: Int) {
  def margin = Math.abs(homeScore - awayScore)
}

class ResultRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  private val Results = TableQuery[ResultsTable]

  private class ResultsTable(tag: Tag) extends Table[Result](tag, "RESULT") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def gameId = column[Long]("GAME_ID")

    def homeScore = column[Int]("HOME_SCORE")

    def awayScore = column[Int]("AWAY_SCORE")

    def periods = column[Int]("PERIODS")


    def * = (id, gameId, homeScore, awayScore, periods) <>(Result.tupled, Result.unapply)

  }

}


