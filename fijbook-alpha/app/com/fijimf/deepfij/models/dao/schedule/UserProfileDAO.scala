package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.UserProfileData

import scala.concurrent.Future

trait UserProfileDAO {

  def deleteUserProfile(userId: String): Future[Int]

  def findUserProfile(userId: String): Future[Map[String, String]]

  def saveUserProfile(userId: String, data: Map[String, String]):Future[Map[String, String]]

  def listUserProfiles: Future[List[UserProfileData]]
}
