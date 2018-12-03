package com.fijimf.deepfij.models.dao.schedule

import java.time.LocalDate

import com.fijimf.deepfij.models.nstats.SnapshotDbBundle
import com.fijimf.deepfij.models.{Season, StatValue, XStat}

import scala.concurrent.Future

trait StatValueDAO {

  def findXStatsTimeSeries(seasonId: Long, teamId: Long, modelKey: String): Future[List[XStat]]

  def findXStatsSnapshot(seasonId: Long, date: LocalDate, modelKey: String): Future[List[XStat]]

  def findXStatsLatest(seasonId: Long, teamId:Long, modelKey: String): Future[Option[XStat]]

  def listXStats: Future[List[XStat]]

  def saveXStats(xstats: List[XStat]): Future[List[Int]]

  def saveXStat(xstat: XStat): Future[Int]

  def saveXStatSnapshot(d: LocalDate, k: String, xstats: List[XStat]): Future[Int]

  def saveBatchedSnapshots(snaps: List[SnapshotDbBundle]): Future[Option[Int]]

  def insertSnapshots(snaps: List[SnapshotDbBundle]): Future[Option[Int]]

  def deleteXStatBySeason(season: Season, key: String): Future[Int]
}
