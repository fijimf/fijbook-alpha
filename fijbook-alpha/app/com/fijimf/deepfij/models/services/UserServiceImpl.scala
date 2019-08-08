package com.fijimf.deepfij.models.services

import java.util.UUID

import cats.effect.IO
import com.fijimf.deepfij.model.auth.User
import com.mohiva.play.silhouette.api.LoginInfo
import doobie.implicits._
import doobie.util.{Get, transactor}
import javax.inject.Inject
import modules.TransactorCtx

import scala.concurrent.Future


class UserServiceImpl @Inject()(transactorCtx: TransactorCtx) extends UserService {

  implicit val natGet: Get[UUID] = Get[String].map(UUID.fromString)
  val xa: transactor.Transactor[IO] = transactorCtx.xa
  val dao: User.Dao[IO] = User.Dao(xa)

  import com.fijimf.deepfij.model.ModelDao._
  override def retrieve(id: UUID):Future[Option[User]] = {
    (dao.select ++ dao.idPredicate(id)).query[User].option.transact(xa).unsafeToFuture()
  }

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    (dao.select ++ fr""" WHERE provider_id = ${loginInfo.providerID} AND provider_key = ${loginInfo.providerKey}""").query[User].option.transact(xa).unsafeToFuture()
  }

  override def save(user: User): Future[User] = {
    sql"""
        INSERT INTO "user" (
          user_id,
          provider_id,
          provider_key,
          first_name,
          last_name,
          full_name,
          email,
          avatar_url,
          activated
        ) VALUES(
          ${user.userID},
          ${user.providerId},
          ${user.providerKey},
          ${user.firstName},
          ${user.lastName},
          ${user.fullName},
          ${user.email},
          ${user.avatarURL},
          ${user.activated}
        ) ON CONFLICT (user_id) DO UPDATE SET
          provider_id=${user.providerId},
          provider_key=${user.providerKey},
          first_name=${user.firstName},
          last_name=${user.lastName},
          full_name=${user.fullName},
          email=${user.email},
          avatar_url=${user.avatarURL},
          activated=${user.activated}
        RETURNING user_id, provider_id, provider_key, first_name, last_name , full_name , email , avatar_url, activated"""
      .update
      .withUniqueGeneratedKeys[User]("user_id", "provider_id", "provider_key", "first_name", "last_name", "full_name", "email", "avatar_url", "activated")
      .transact(xa)
      .unsafeToFuture()
  }

   override def list: Future[List[User]] = {
    dao.select.query[User].to[List].transact(xa).unsafeToFuture()
  }
}
