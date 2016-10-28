package com.fijimf.deepfij.models

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo

import scala.concurrent.Future

/**
  * Give access to the user object.
  */
trait TeamDAO {

  def find(key: String): Future[Option[Team]]

  def find(id: Long): Future[Option[Team]]

  def save(team: Team): Future[Int]

  def list: Future[List[Team]]
}