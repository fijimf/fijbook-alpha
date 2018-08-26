package com.fijimf.deepfij.models.services

import java.time.LocalDate

import com.fijimf.deepfij.models.{RssItem, Season}
import controllers.GameMapping

import scala.concurrent.Future
import scala.language.postfixOps

trait RssFeedUpdateService {

  def updateFeed(id:Long):Future[List[RssItem]]

}