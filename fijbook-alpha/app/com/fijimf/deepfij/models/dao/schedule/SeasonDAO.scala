package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.Season

import scala.concurrent.Future

trait SeasonDAO {

  def deleteSeason(id: Long): Future[Int]

  def saveSeason(season: Season): Future[Season]

  def saveSeasons(seasons: List[Season]): Future[List[Season]]

  def listSeasons: Future[List[Season]]

  def findSeasonById(id: Long): Future[Option[Season]]

  def findSeasonByYear(year: Int): Future[Option[Season]]
}
