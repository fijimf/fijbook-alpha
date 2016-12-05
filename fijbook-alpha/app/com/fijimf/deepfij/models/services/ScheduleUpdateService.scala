package com.fijimf.deepfij.models.services

import java.util.UUID

import com.fijimf.deepfij.models.AuthToken

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Handles actions to auth tokens.
  */
trait ScheduleUpdateService {

  def update()

}