package com.fijimf.deepfij.auth.services

import java.util.UUID

import com.fijimf.deepfij.auth.model.AuthToken

import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}

trait AuthTokenService {
  def create(userID: UUID, expiry: FiniteDuration = 5 minutes): Future[AuthToken]

  def validate(id: UUID): Future[Option[AuthToken]]

  def clean: Future[Seq[AuthToken]]
}
