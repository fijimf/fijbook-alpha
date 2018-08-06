package com.fijimf.deepfij.models

import java.util.UUID

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import org.apache.commons.lang3.StringUtils

/**
  * The user object.
  *
  * @param userID The unique ID of the user.
  * @param loginInfo The linked login info.
  * @param firstName Maybe the first name of the authenticated user.
  * @param lastName Maybe the last name of the authenticated user.
  * @param fullName Maybe the full name of the authenticated user.
  * @param email Maybe the email of the authenticated provider.
  * @param avatarURL Maybe the avatar URL of the authenticated provider.
  */
case class User(
                 userID: UUID,
                 loginInfo: LoginInfo,
                 firstName: Option[String],
                 lastName: Option[String],
                 fullName: Option[String],
                 email: Option[String],
                 avatarURL: Option[String],
                 activated: Boolean) extends Identity {

  /**
    * Tries to construct a name.
    *
    * @return Maybe a name.
    */
  def name = fullName.getOrElse {
    firstName -> lastName match {
      case (Some(f), Some(l)) => f + " " + l
      case (Some(f), None) => f
      case (None, Some(l)) => l
      case _ => StringUtils.abbreviate(userID.toString,10)
    }
  }

  def isDeepFijAdmin:Boolean = Option(System.getProperty("deepfij.admin.emails")).exists(_.split(",").contains(email))

}


