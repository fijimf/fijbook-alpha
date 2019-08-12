package com.fijimf.deepfij.auth.services

import cats.effect.IO
import com.fijimf.deepfij.auth.model.Password
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import doobie.implicits._
import doobie.util.{Get, Read, transactor}
import javax.inject.Inject
import modules.TransactorCtx

import scala.concurrent.Future

class PasswordInfoServiceImpl @Inject()(transactorCtx: TransactorCtx) extends DelegableAuthInfoDAO[PasswordInfo] {

  implicit val piRead:Read[PasswordInfo]=Read[(String, String, Option[String])].map(t=>new PasswordInfo(t._1, t._2,t._3))
  val xa: transactor.Transactor[IO] = transactorCtx.xa
  val passwordDao: Password.Dao[IO] = Password.Dao(xa)

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    passwordDao.findByLoginInfo(loginInfo).option.transact(xa).unsafeToFuture()


  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    passwordDao.insert(loginInfo, authInfo)
      .withUniqueGeneratedKeys[PasswordInfo]("id", "hasher", "password", "salt", "key")
      .transact(xa)
      .unsafeToFuture()
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    passwordDao.updateByLoginInfo(loginInfo, authInfo)
      .withUniqueGeneratedKeys[PasswordInfo]("id", "hasher", "password", "salt", "key")
      .transact(xa)
      .unsafeToFuture()
  }

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    passwordDao.upsert(loginInfo, authInfo)
      .withUniqueGeneratedKeys[PasswordInfo]("id", "hasher", "password", "salt", "key")
      .transact(xa)
      .unsafeToFuture()
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = {
    passwordDao.delete(loginInfo).run.transact(xa).map(_ => ()).unsafeToFuture()
  }
}
