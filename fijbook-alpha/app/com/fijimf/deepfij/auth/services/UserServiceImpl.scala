package com.fijimf.deepfij.auth.services

import java.util.UUID

import cats.effect.IO
import com.fijimf.deepfij.auth.model.User
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
  override def retrieve(id: UUID):Future[Option[User]] = {
    dao.findById(id).option.transact(xa).unsafeToFuture()
  }

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    dao.findByLoginInfo(loginInfo).option.transact(xa).unsafeToFuture()
  }

  override def save(user: User): Future[User] = {
    dao.save(user)
      .withUniqueGeneratedKeys[User]("user_id", "provider_id", "provider_key", "first_name", "last_name", "full_name", "email", "avatar_url", "activated")
      .transact(xa)
      .unsafeToFuture()
  }

   override def list: Future[List[User]] = {
    dao.list.to[List].transact(xa).unsafeToFuture()
  }
}
