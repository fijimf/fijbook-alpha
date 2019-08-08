package com.fijimf.deepfij.models.services

import java.util.UUID

import com.fijimf.deepfij.model.auth.User
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService

import scala.concurrent.Future

trait UserService extends IdentityService[User] {
  def save(user: User): Future[User]
  def retrieve(id: UUID):Future[Option[User]]
  def retrieve(loginInfo:LoginInfo):Future[Option[User]]
  def list:Future[List[User]]
}

