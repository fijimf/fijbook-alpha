package com.fijimf.deepfij.model.auth

import java.util.UUID

import cats.effect.IO._
import cats.effect.{Bracket, IO}
import com.fijimf.deepfij.model.ModelDao
import com.mohiva.play.silhouette.api.{AuthInfo, LoginInfo}
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.{Get, transactor, _}
import javax.inject.Inject
import modules.TransactorCtx

import scala.concurrent.Future

case class PasswordInfo
(
  id: Long,
  hasher: String,
  password: String,
  salt: Option[String],
  providerId: String,
  providerKey: String,
) extends AuthInfo {
  val loginInfo = new LoginInfo(providerId, providerKey)
}

object PasswordInfo {

  case class Dao[M[_]](xa: Transactor[M])(implicit M: Bracket[M, Throwable]) extends ModelDao[PasswordInfo, Long] {
    override def createDdl: doobie.ConnectionIO[Int] =
      sql"""
        CREATE TABLE password_info (
           id BIGSERIAL PRIMARY KEY,
           hasher VARCHAR(36) NOT NULL,
           password VARCHAR(64) NOT NULL,
           salt VARCHAR(64) NULL,
            provider_id VARCHAR(36) NOT NULL,
           provider_key VARCHAR(36) NOT NULL
         );
         CREATE UNIQUE INDEX ON password_info(provider_id, provider_key);
 """.update.run


    override def dropDdl: doobie.ConnectionIO[Int] =
      sql"""
        DROP TABLE IF EXISTS password_info
        """.update.run


    override def select: fragment.Fragment = fr"""SELECT id, hasher, password, salt, provider_id, provider_key FROM password_info """

    override def delete: fragment.Fragment = fr"""DELETE FROM password_info """


    def findByLoginInfo(loginInfo: LoginInfo): doobie.Query0[PasswordInfo] = {
      (select ++ loginInfoPredicate(loginInfo)).query[PasswordInfo]
    }

    private def loginInfoPredicate(loginInfo: LoginInfo) = {
      fr"""WHERE provider_id=${loginInfo.providerID} AND provider_key=${loginInfo.providerKey}"""
    }

    def insert(pi: PasswordInfo): doobie.Update0 = {
      sql"""
        INSERT INTO password_info(hasher, password, salt, provider_id, provider_key) values
        (
          ${pi.hasher},${pi.password},${pi.salt},${pi.providerId},${pi.providerKey}
        ) RETURNING id, hasher, password, salt, provider_id, provider_key
        """.update
    }


    def updateByLoginInfo(pi: PasswordInfo): doobie.Update0 = {
      (fr"""
        UPDATE password_info SET hasher=${pi.hasher}, password=${pi.password}, salt=${pi.salt}
        """ ++ loginInfoPredicate(pi.loginInfo)).update

    }

    def upsert(pi: PasswordInfo):doobie.Update0 = {
      sql"""
        INSERT INTO password_info(hasher, password, salt, provider_id, provider_key) values
        (
          ${pi.hasher},${pi.password},${pi.salt},${pi.providerId},${pi.providerKey}
        ) ON CONFLICT (provider_id, provider_key) DO UPDATE SET hasher=${pi.hasher}, password=${pi.password}, salt=${pi.salt}
        RETURNING id, hasher, password, salt, provider_id, provider_key
        """.update
    }

    def delete(loginInfo: LoginInfo): doobie.Update0 = {
      (delete ++ loginInfoPredicate(loginInfo)).update
    }
  }

}

class PasswordInfoService @Inject()(transactorCtx: TransactorCtx) extends DelegableAuthInfoDAO[PasswordInfo] {

  val xa: transactor.Transactor[IO] = transactorCtx.xa
  val passwordDao: PasswordInfo.Dao[IO] = PasswordInfo.Dao(xa)

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    passwordDao.findByLoginInfo(loginInfo).option.transact(xa).unsafeToFuture()


  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    passwordDao.insert(authInfo.copy(providerId = loginInfo.providerID, providerKey = loginInfo.providerKey))
      .withUniqueGeneratedKeys[PasswordInfo]("id", "hasher", "password", "salt", "provider_id", "provider_key")
      .transact(xa)
      .unsafeToFuture()
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    val pi = authInfo.copy(providerId = loginInfo.providerID, providerKey = loginInfo.providerKey)
    passwordDao.updateByLoginInfo(pi)
      .run
      .transact(xa).map(_ => pi)
      .unsafeToFuture()
  }

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    passwordDao.upsert(authInfo.copy(providerId = loginInfo.providerID, providerKey = loginInfo.providerKey)).withUniqueGeneratedKeys[PasswordInfo]("id", "hasher", "password", "salt", "provider_id", "provider_key")
      .transact(xa)
      .unsafeToFuture()
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = {
    passwordDao.delete(loginInfo).run.transact(xa).map(_=>()).unsafeToFuture()
  }
}

