package com.fijimf.deepfij.models.dao.schedule

import java.time.LocalDate

import com.fijimf.deepfij.models.{StatValue, XStat}

import scala.concurrent.Future

trait StatValueDAO {

  def listStatValues: Future[List[StatValue]]

  def deleteStatValues(dates: List[LocalDate], model: List[String]): Future[Int]

  def saveStatValues(dates: List[LocalDate], model: List[String], stats: List[StatValue]):  Future[Seq[Long]]

  def loadStatValues(statKey: String, modelKey: String): Future[List[StatValue]]

  def loadStatValues(modelKey: String): Future[List[StatValue]]

  def loadStatValues(modelKey: String, from:LocalDate, to:LocalDate): Future[List[StatValue]]

  def saveXStats(xstats: List[XStat]): Future[List[XStat]]

  def saveXStat(xstat: XStat): Future[XStat]
}
