package com.fijimf.deepfij.auth.model

import java.util.UUID

import org.joda.time.DateTime

final case class AuthToken(id: UUID, userID: UUID, expiry: DateTime)
