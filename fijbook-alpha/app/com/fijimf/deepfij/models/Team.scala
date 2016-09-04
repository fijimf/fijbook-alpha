package com.fijimf.deepfij.models

import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.Future

case class Team(id: Long, key: String, name: String, longName: String, nickname: String, logoLgUrl: Option[String], logoSmUrl: Option[String], primaryColor: Option[String], secondaryColor: Option[String], officialUrl: Option[String], officialTwitter: Option[String], officialFacebook: Option[String])

class TeamRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  def all: Future[List[Team]] = db.run(Teams.to[List].result)

  def findById(id: Long): Future[Team] = db.run(Teams.filter(_.id === id).result.head)

  def create(key: String, name: String, longName: String, nickname: String,
             logoLgUrl: Option[String] = None, logoSmUrl: Option[String] = None,
             primaryColor: Option[String] = None, secondaryColor: Option[String] = None,
             officialUrl: Option[String] = None, officialTwitter: Option[String] = None, officialFacebook: Option[String] = None): Future[Long] = {
    val t = Team(0, key, name, longName, nickname, logoLgUrl, logoSmUrl, primaryColor, secondaryColor, officialUrl, officialTwitter, officialFacebook)
    db.run(Teams returning Teams.map(_.id) += t)
  }

  private val Teams = TableQuery[TeamsTable]

  private class TeamsTable(tag: Tag) extends Table[Team](tag, "TEAM") {

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

}

