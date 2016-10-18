package com.fijimf.deepfij.models

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.ProvenShape.proveShapeOf

import scala.concurrent.{ExecutionContext, Future}


case class DBUser(
                   userID: String,
                   firstName: Option[String],
                   lastName: Option[String],
                   fullName: Option[String],
                   email: Option[String],
                   avatarURL: Option[String],
                   activated:Boolean
                 )

case class DBLoginInfo(
                        id: Option[Long],
                        providerID: String,
                        providerKey: String
                      )

case class DBUserLoginInfo(
                            userID: String,
                            loginInfoId: Long
                          )

case class DBPasswordInfo(
                           hasher: String,
                           password: String,
                           salt: Option[String],
                           loginInfoId: Long
                         )

case class DBOAuth1Info(
                         id: Option[Long],
                         token: String,
                         secret: String,
                         loginInfoId: Long
                       )

case class DBOAuth2Info(
                         id: Option[Long],
                         accessToken: String,
                         tokenType: Option[String],
                         expiresIn: Option[Int],
                         refreshToken: Option[String],
                         loginInfoId: Long
                       )

case class DBOpenIDInfo(
                         id: String,
                         loginInfoId: Long
                       )

case class DBOpenIDAttribute(
                              id: String,
                              key: String,
                              value: String
                            )

class UserRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val log = Logger("schedule-repo")
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  def dumpSchema()(implicit ec: ExecutionContext) = {
    Future((ddl.create.statements, ddl.drop.statements))
  }

  def createSchema() = {
    log.warn("Creating user schema")
    db.run(ddl.create.transactionally)
  }

  def dropSchema() = {
    log.warn("Dropping user schema")
    db.run(ddl.drop.transactionally)
  }


  class Users(tag: Tag) extends Table[DBUser](tag, "user") {
    def id = column[String]("userID", O.PrimaryKey)

    def firstName = column[Option[String]]("firstName")

    def lastName = column[Option[String]]("lastName")

    def fullName = column[Option[String]]("fullName")

    def email = column[Option[String]]("email")

    def avatarURL = column[Option[String]]("avatarURL")

    def activated = column[Boolean]("activated")

    def * = (id, firstName, lastName, fullName, email, avatarURL, activated) <> (DBUser.tupled, DBUser.unapply)
  }


  class LoginInfos(tag: Tag) extends Table[DBLoginInfo](tag, "logininfo") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def providerID = column[String]("providerID")

    def providerKey = column[String]("providerKey")

    def * = (id.?, providerID, providerKey) <> (DBLoginInfo.tupled, DBLoginInfo.unapply)
  }

  class UserLoginInfos(tag: Tag) extends Table[DBUserLoginInfo](tag, "userlogininfo") {
    def userID = column[String]("userID")

    def loginInfoId = column[Long]("loginInfoId")

    def * = (userID, loginInfoId) <> (DBUserLoginInfo.tupled, DBUserLoginInfo.unapply)
  }


  class PasswordInfos(tag: Tag) extends Table[DBPasswordInfo](tag, "passwordinfo") {
    def hasher = column[String]("hasher")

    def password = column[String]("password")

    def salt = column[Option[String]]("salt")

    def loginInfoId = column[Long]("loginInfoId")

    def * = (hasher, password, salt, loginInfoId) <> (DBPasswordInfo.tupled, DBPasswordInfo.unapply)
  }


  class OAuth1Infos(tag: Tag) extends Table[DBOAuth1Info](tag, "oauth1info") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def token = column[String]("token")

    def secret = column[String]("secret")

    def loginInfoId = column[Long]("loginInfoId")

    def * = (id.?, token, secret, loginInfoId) <> (DBOAuth1Info.tupled, DBOAuth1Info.unapply)
  }


  class OAuth2Infos(tag: Tag) extends Table[DBOAuth2Info](tag, "oauth2info") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def accessToken = column[String]("accesstoken")

    def tokenType = column[Option[String]]("tokentype")

    def expiresIn = column[Option[Int]]("expiresin")

    def refreshToken = column[Option[String]]("refreshtoken")

    def loginInfoId = column[Long]("logininfoid")

    def * = (id.?, accessToken, tokenType, expiresIn, refreshToken, loginInfoId) <> (DBOAuth2Info.tupled, DBOAuth2Info.unapply)
  }


  class OpenIDInfos(tag: Tag) extends Table[DBOpenIDInfo](tag, "openidinfo") {
    def id = column[String]("id", O.PrimaryKey)

    def loginInfoId = column[Long]("logininfoid")

    def * = (id, loginInfoId) <> (DBOpenIDInfo.tupled, DBOpenIDInfo.unapply)
  }


  class OpenIDAttributes(tag: Tag) extends Table[DBOpenIDAttribute](tag, "openidattributes") {
    def id = column[String]("id")

    def key = column[String]("key")

    def value = column[String]("value")

    def * = (id, key, value) <> (DBOpenIDAttribute.tupled, DBOpenIDAttribute.unapply)
  }

  // table query definitions
  lazy val users = TableQuery[Users]
  val loginInfos = TableQuery[LoginInfos]
  val userLoginInfos = TableQuery[UserLoginInfos]
  val passwordInfos = TableQuery[PasswordInfos]
  val oAuth1Infos = TableQuery[OAuth1Infos]
  val oAuth2Infos = TableQuery[OAuth2Infos]
  val openIDInfos = TableQuery[OpenIDInfos]
  val openIDAttributes = TableQuery[OpenIDAttributes]

  lazy val ddl = users.schema ++ loginInfos.schema ++ userLoginInfos.schema ++ passwordInfos.schema ++ oAuth1Infos.schema ++ oAuth2Infos.schema ++ openIDInfos.schema ++ openIDAttributes.schema

  // queries used in multiple places
  def loginInfoQuery(loginInfo: LoginInfo) =
  loginInfos.filter(dbLoginInfo => dbLoginInfo.providerID === loginInfo.providerID && dbLoginInfo.providerKey === loginInfo.providerKey)
}

