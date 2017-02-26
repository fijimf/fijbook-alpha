package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{ScheduleRepository, Team, UserProfileData}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait UserProfileDAOImpl extends UserProfileDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository
  import dbConfig.driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def deleteUserProfile(userId: String): Future[Int] = db.run(repo.userProfiles.filter(_.userId===userId).delete)

  override def findUserProfile(userId: String): Future[Map[String, String]] = db.run(repo.userProfiles.filter(_.userId===userId).to[List].result).map(_.map(u=>u.key->u.value).toMap)

  override def saveUserProfile(userId: String, data: Map[String, String]): Future[Map[String, String]] = {
    val userProfileDatas = data.map {
      case (k: String, v: String) => UserProfileData(0L, userId, k, v)
    }
    val del = repo.userProfiles.filter(_.userId===userId).delete
    val ins = repo.userProfiles ++= userProfileDatas
    db.run(del.andThen(ins).transactionally).map(_=>data)
  }

  override def listUserProfiles: Future[List[UserProfileData]] = db.run(repo.userProfiles.to[List].result)
}
