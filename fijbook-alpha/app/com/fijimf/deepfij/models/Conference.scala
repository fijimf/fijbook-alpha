package com.fijimf.deepfij.models

import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.Future

case class Conference(id: Long, key: String, name: String, logoLgUrl: Option[String], logoSmUrl: Option[String], officialUrl: Option[String], officialTwitter: Option[String], officialFacebook: Option[String])

class ConferenceRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  def all: Future[List[Conference]] = db.run(Conferences.to[List].result)

  def findById(id: Long): Future[Conference] = db.run(Conferences.filter(_.id === id).result.head)

  def create(key: String, name: String,
             logoLgUrl: Option[String] = None, logoSmUrl: Option[String] = None,
             officialUrl: Option[String] = None, officialTwitter: Option[String] = None, officialFacebook: Option[String] = None): Future[Long] = {
    val t = Conference(0, key, name, logoLgUrl, logoSmUrl, officialUrl, officialTwitter, officialFacebook)
    db.run(Conferences returning Conferences.map(_.id) += t)
  }

  private val Conferences = TableQuery[ConferencesTable]

  private class ConferencesTable(tag: Tag) extends Table[Conference](tag, "CONFERENCE") {

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

}

