package com.fijimf.deepfij.models.dao.silhouette

import java.util.UUID

import akka.actor.ActorSystem
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import com.mohiva.play.silhouette.api.LoginInfo
import javax.inject.Inject
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

/**
  * Give access to the user object using Slick
  */
class UserDAOImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, protected val userRepo:UserRepository,val actorSystem: ActorSystem) (implicit ec: ExecutionContext)extends UserDAO with DAOSlick {

  val log = Logger(this.getClass)

  import dbConfig.profile.api._
  /**
    * Finds a user by its login info.
    *
    * @param loginInfo The login info of the user to find.
    * @return The found user or None if no user for the given login info could be found.
    */
  def find(loginInfo: LoginInfo) = {
    val userQuery = for {
      dbLoginInfo <- userRepo.loginInfoQuery(loginInfo)
      dbUserLoginInfo <- userRepo.userLoginInfos.filter(_.loginInfoID === dbLoginInfo.id)
      dbUser <- userRepo.users.filter(_.id === dbUserLoginInfo.userID)
    } yield dbUser
    db.run(userQuery.result.headOption).map { dbUserOption =>
      dbUserOption.map { user =>
        User(UUID.fromString(user.userID), loginInfo, user.firstName, user.lastName, user.fullName, user.email, user.avatarURL, user.activated)
      }
    }
  }

  /**
    * Finds a user by its user ID.
    *
    * @param userID The ID of the user to find.
    * @return The found user or None if no user for the given ID could be found.
    */
  def find(userID: UUID) = {
    val query = for {
      dbUser <- userRepo.users.filter(_.id === userID.toString)
      dbUserLoginInfo <- userRepo.userLoginInfos.filter(_.userID === dbUser.id)
      dbLoginInfo <- userRepo.loginInfos.filter(_.id === dbUserLoginInfo.loginInfoID)
    } yield (dbUser, dbLoginInfo)
    db.run(query.result.headOption).map { resultOption =>
      resultOption.map {
        case (user, loginInfo) =>
          User(
            UUID.fromString(user.userID),
            LoginInfo(loginInfo.providerID, loginInfo.providerKey),
            user.firstName,
            user.lastName,
            user.fullName,
            user.email,
            user.avatarURL,
            user.activated)
      }
    }
  }

  /**
    * Saves a user.
    *
    * @param user The user to save.
    * @return The saved user.
    */
  def save(user: User) = {
    val dbUser = DBUser(user.userID.toString, user.firstName, user.lastName, user.fullName, user.email, user.avatarURL, user.activated)
    val dbLoginInfo = DBLoginInfo(None, user.loginInfo.providerID, user.loginInfo.providerKey)
    // We don't have the LoginInfo id so we try to get it first.
    // If there is no LoginInfo yet for this user we retrieve the id on insertion.
    val loginInfoAction = {
      val retrieveLoginInfo = userRepo.loginInfos.filter(
        info => info.providerID === user.loginInfo.providerID &&
          info.providerKey === user.loginInfo.providerKey).result.headOption
      val insertLoginInfo = userRepo.loginInfos.returning(userRepo.loginInfos.map(_.id)).
        into((info, id) => info.copy(id = Some(id))) += dbLoginInfo
      for {
        loginInfoOption <- retrieveLoginInfo
        loginInfo <- loginInfoOption.map(DBIO.successful(_)).getOrElse(insertLoginInfo)
      } yield loginInfo
    }
    // combine database actions to be run sequentially
    val actions = (for {
      _ <- userRepo.users.insertOrUpdate(dbUser)
      loginInfo <- loginInfoAction
      _ <- userRepo.userLoginInfos.insertOrUpdate(DBUserLoginInfo(dbUser.userID, loginInfo.id.get))
    } yield ()).transactionally
    // run actions and return user afterwards
    db.run(actions).map(_ => user)
  }

  override def list: Future[List[User]] = {
    val userQuery = for {
      dbUser <- userRepo.users;
      dbUserLoginInfo <- userRepo.userLoginInfos.filter(_.userID === dbUser.id)
      dbLoginInfo <- userRepo.loginInfos.filter(_.id===dbUserLoginInfo.loginInfoID)
    } yield {
      (dbUser, dbLoginInfo)
    }
    db.run(userQuery.result).map(_.map{case (user: DBUser, info: DBLoginInfo) =>
      User(
        UUID.fromString(user.userID),
        LoginInfo(info.providerID, info.providerKey),
        user.firstName, user.lastName, user.fullName, user.email, user.avatarURL, user.activated
      )
    }.toList)
  }
}