package com.fijimf.deepfij.auth.services

import java.util.UUID

import com.fijimf.deepfij.auth.model
import com.fijimf.deepfij.auth.model.AuthToken
import com.mohiva.play.silhouette.api.util.Clock
import javax.inject.Inject
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AuthTokenServiceImpl @Inject()(clock: Clock)(implicit ec: ExecutionContext) extends AuthTokenService {

  def create(userID: UUID, expiry: FiniteDuration = 5 minutes): Future[AuthToken] = {
    val token = AuthToken(UUID.randomUUID(), userID, clock.now.withZone(DateTimeZone.UTC).plusSeconds(expiry.toSeconds.toInt))
    AuthTokenDAO.save(token)
  }

  def validate(id: UUID): Future[Option[model.AuthToken]] = AuthTokenDAO.find(id)

  def clean: Future[Seq[model.AuthToken]] = AuthTokenDAO.findExpired(clock.now.withZone(DateTimeZone.UTC)).flatMap { tokens =>
    Future.sequence(tokens.map { token =>
      AuthTokenDAO.remove(token.id).map(_ => token)
    })
  }

}


object AuthTokenDAO {
  private[this] val tokens: mutable.HashMap[UUID, model.AuthToken] = mutable.HashMap()

  def find(id: UUID): Future[Option[model.AuthToken]] = Future.successful(tokens.get(id))

  def findExpired(dateTime: DateTime): Future[Seq[model.AuthToken]] = Future.successful {
    tokens.filter {
      case (id, token) =>
        token.expiry.isBefore(dateTime)
    }.values.toSeq
  }

  def save(token: model.AuthToken): Future[model.AuthToken] = {
    tokens += (token.id -> token)
    Future.successful(token)
  }

  def remove(id: UUID): Future[Unit] = {
    tokens -= id
    Future.successful(())
  }
}

