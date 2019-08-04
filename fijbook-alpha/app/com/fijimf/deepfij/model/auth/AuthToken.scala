package com.fijimf.deepfij.model.auth

import java.util.UUID

import org.joda.time.DateTime

final case class AuthToken(id: UUID, userID: UUID, expiry: DateTime)