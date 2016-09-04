package com.fijimf.deepfij.models

import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.Future

case class ConferenceMap(id: Long, seasonId:Long, conferenceId:Long, teamId:Long)

class ConferenceMapRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  def all: Future[List[ConferenceMap]] = db.run(ConferenceMaps.to[List].result)

  def findById(id: Long): Future[ConferenceMap] = db.run(ConferenceMaps.filter(_.id === id).result.head)

  def create(seasonId:Long, conferenceId:Long, teamId:Long): Future[Long] = {
    val t = ConferenceMap(0, seasonId, conferenceId, teamId)
    db.run(ConferenceMaps returning ConferenceMaps.map(_.id) += t)
  }

  private val ConferenceMaps = TableQuery[ConferenceMapsTable]

  private class ConferenceMapsTable(tag: Tag) extends Table[ConferenceMap](tag, "CONFERENCE_MAP") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def seasonId = column[Long]("SEASON_ID" )
    def conferenceId = column[Long]("CONFERENCE_ID")
    def teamId = column[Long]("TEAM_ID")

    def * = (id,  seasonId, conferenceId, teamId) <>(ConferenceMap.tupled, ConferenceMap.unapply)

  }

}

