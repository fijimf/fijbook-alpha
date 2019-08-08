package com.fijimf.deepfij.models.dao.silhouette

import akka.actor.ActorSystem
import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{DBPasswordInfo, UserRepository}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import javax.inject.Inject
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

/**
  * The DAO to store the password information.
  */
class PasswordInfoDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, protected val userRepo:UserRepository, val actorSystem: ActorSystem)(implicit ec: ExecutionContext)
  extends DelegableAuthInfoDAO[PasswordInfo] with DAOSlick {

  val log = Logger(this.getClass)

  import dbConfig.profile.api._

  protected def passwordInfoQuery(loginInfo: LoginInfo) = for {
    dbLoginInfo <- userRepo.loginInfoQuery(loginInfo)
    dbPasswordInfo <- userRepo.passwordInfos if dbPasswordInfo.loginInfoId === dbLoginInfo.id
  } yield dbPasswordInfo

  protected def passwordInfoSubQuery(loginInfo: LoginInfo) =
  userRepo.passwordInfos.filter(_.loginInfoId in userRepo.loginInfoQuery(loginInfo).map(_.id))

  protected def addAction(loginInfo: LoginInfo, authInfo: PasswordInfo) =
    userRepo.loginInfoQuery(loginInfo).result.head.flatMap { dbLoginInfo =>
      userRepo.passwordInfos +=
        DBPasswordInfo(authInfo.hasher, authInfo.password, authInfo.salt, dbLoginInfo.id.get)
    }.transactionally

  protected def updateAction(loginInfo: LoginInfo, authInfo: PasswordInfo) =
    passwordInfoSubQuery(loginInfo).
      map(dbPasswordInfo => (dbPasswordInfo.hasher, dbPasswordInfo.password, dbPasswordInfo.salt)).
      update((authInfo.hasher, authInfo.password, authInfo.salt))

  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    db.run(passwordInfoQuery(loginInfo).result.headOption).map { dbPasswordInfoOption =>
      dbPasswordInfoOption.map(dbPasswordInfo =>
        PasswordInfo(dbPasswordInfo.hasher, dbPasswordInfo.password, dbPasswordInfo.salt))
    }
  }

  def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
  db.run(addAction(loginInfo, authInfo)).map(_ => authInfo)

  def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
  db.run(updateAction(loginInfo, authInfo)).map(_ => authInfo)

  def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    val query = userRepo.loginInfoQuery(loginInfo).joinLeft(userRepo.passwordInfos).on(_.id === _.loginInfoId)
    val action = query.result.head.flatMap {
      case (dbLoginInfo, Some(dbPasswordInfo)) => updateAction(loginInfo, authInfo)
      case (dbLoginInfo, None) => addAction(loginInfo, authInfo)
    }
    db.run(action).map(_ => authInfo)
  }

  def remove(loginInfo: LoginInfo): Future[Unit] =
  db.run(passwordInfoSubQuery(loginInfo).delete).map(_ => ())
}
