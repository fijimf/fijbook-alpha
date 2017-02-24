package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.Season

import scala.concurrent.Future

/**
  * Created by jimfrohnhofer on 2/22/17.
  */
trait SeasonDAO {

  def deleteSeason(id: Long): Future[Int]

  def unlockSeason(seasonId: Long): Future[Int]

  def checkAndSetLock(seasonId: Long): Boolean

  def saveSeason(season: Season): Future[Season]

  def listSeasons: Future[List[Season]]

  def findSeasonById(id: Long): Future[Option[Season]]

  def findSeasonByYear(year: Int): Future[Option[Season]]
}
