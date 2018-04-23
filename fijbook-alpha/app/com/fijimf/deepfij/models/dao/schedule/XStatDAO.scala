package com.fijimf.deepfij.models.dao.schedule

import java.time.LocalDate

import com.fijimf.deepfij.models.{StatValue, XStat}

import scala.concurrent.Future

trait XStatDAO {
  
  def createXStatsFromSparkDump:Future[Boolean]

  def listStatValues: Future[List[XStat]]
  
//  def loadStatValues(statKey: String, modelKey: String): Future[List[StatValue]]
//
//  def loadStatValues(modelKey: String): Future[List[StatValue]]
//
//  def loadStatValues(modelKey: String, from:LocalDate, to:LocalDate): Future[List[StatValue]]
}
