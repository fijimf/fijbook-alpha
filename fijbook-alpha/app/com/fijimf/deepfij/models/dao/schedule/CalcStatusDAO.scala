package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.{Alias, CalcStatus}

import scala.concurrent.Future

trait CalcStatusDAO {

  def findStatus(seasonId:Long, modelKey:String):  Future[Option[CalcStatus]]

  def saveStatus(stat:CalcStatus): Future[CalcStatus]

  def deleteStatus(id: Long): Future[Int]
}
