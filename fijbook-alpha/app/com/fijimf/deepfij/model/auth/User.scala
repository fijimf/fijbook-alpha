package com.fijimf.deepfij.model.auth

import java.util.UUID

import cats.effect.Bracket
import cats.implicits._
import com.fijimf.deepfij.model.ModelDao
import com.mohiva.play.silhouette.api.{Identity}
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.{Get, Put, fragment}
import org.apache.commons.lang3.StringUtils


final case class User
(
  userID: UUID,
  providerId: String,
  providerKey: String,
  firstName: Option[String],
  lastName: Option[String],
  fullName: Option[String],
  email: Option[String],
  avatarURL: Option[String],
  activated: Boolean) extends Identity {

  def name: String = fullName.getOrElse {
    firstName -> lastName match {
      case (Some(f), Some(l)) => f + " " + l
      case (Some(f), None) => f
      case (None, Some(l)) => l
      case _ => StringUtils.abbreviate(userID.toString, 10)
    }
  }

  def isDeepFijAdmin: Boolean = {
    (for {
      x <- Option(System.getProperty(User.adminUser))
      y <- email
    } yield {
      x.trim.toLowerCase === y.trim.toLowerCase
    }).getOrElse(false)
  }

}

object User {
  val adminUser = "admin.user"
  implicit val uuidGet: Get[UUID] = Get[String].map(UUID.fromString)
  implicit val uuidPut: Put[UUID] = Put[String].contramap(_.toString)

  case class Dao[M[_]](xa: Transactor[M])(implicit M: Bracket[M, Throwable])  extends ModelDao[User, UUID] {

    override def createDdl: doobie.ConnectionIO[Int] =
      sql"""
        CREATE TABLE "user" (
           user_id VARCHAR(36) NOT NULL PRIMARY KEY,
           provider_id VARCHAR(36) NOT NULL,
           provider_key VARCHAR(36) NOT NULL,
           first_name VARCHAR(64) NULL,
           last_name VARCHAR(64) NULL,
           full_name VARCHAR(96) NULL,
           email VARCHAR(96) NULL,
           avatar_url VARCHAR(96) NULL,
           activated BOOLEAN NOT NULL
         );
         CREATE UNIQUE INDEX ON "user"(email);
 """.update.run


    override def dropDdl: doobie.ConnectionIO[Int] =
      sql"""
        DROP TABLE IF EXISTS "user"
        """.update.run

    override def select: fragment.Fragment = fr"""SELECT user_id, provider_id, provider_key, first_name, last_name, full_name, email, avatar_url, activated FROM "user" """

    override def delete: fragment.Fragment = fr"""DELETE FROM "user" """


    override def idPredicate(id: UUID)(implicit uuidPut: Put[UUID]): fragment.Fragment = fr"""WHERE user_id=$id """


    //    def saveUser(u: User): doobie.ConnectionIO[User] = {
    //      import ModelDao._
    //
    //      sql"""
    //          INSERT INTO
    //           "user" (user_id, provider_id, provider_key, first_name, last_name, full_name, email, avatar_url, activated )
    //           VALUES (${u.userID.toString},${u.providerId},${u.providerKey},${u.firstName},${u.lastName},${u.fullName},${u.email},${u.avatarURL}, ${u.activated})
    //           RETURNING user_id, provider_id, provider_key, first_name, last_name, full_name, email, avatar_url, activated
    //        """.updateWithUniqueGeneratedKeys[User]("user_id", "provider_id", "provider_key", "first_name", "last_name", "full_name", "email", "avatar_url", "activated")
    //
    //
    //    }
  }

}
