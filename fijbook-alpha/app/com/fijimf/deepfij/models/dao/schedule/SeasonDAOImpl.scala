package com.fijimf.deepfij.models.dao.schedule

import java.util.concurrent.TimeUnit

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Alias, ScheduleRepository, Season}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


trait SeasonDAOImpl extends SeasonDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository
  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def saveSeason(s: Season): Future[Season] = {
  saveSeasons(List(s)).map(_.head)
  }

  override def saveSeasons(seasons: List[Season]): Future[List[Season]] = {
    val ops = seasons.map(c1 => repo.seasons.filter(c => c.year === c1.year).result.flatMap(cs =>
      cs.headOption match {
        case Some(c) =>
          (repo.seasons returning repo.seasons.map(_.id)).insertOrUpdate(c1.copy(id = c.id))
        case None =>
          (repo.seasons returning repo.seasons.map(_.id)).insertOrUpdate(c1)
      }
    ).flatMap(_ => repo.seasons.filter(t => t.year === c1.year).result.head))
    db.run(DBIO.sequence(ops).transactionally)
  }

  override def findSeasonById(id: Long): Future[Option[Season]] = {
    val q = repo.seasons.filter(season => season.id === id)
    db.run(q.result.headOption)
  }

  override def findSeasonByYear(year: Int): Future[Option[Season]] = {
    val q = repo.seasons.filter(season => season.year === year)
    db.run(q.result.headOption)
  }

  override def deleteSeason(id: Long): Future[Int] = db.run(repo.seasons.filter(season => season.id === id).delete)

  override def unlockSeason(seasonId: Long): Future[Int] = {
    db.run(repo.seasons.filter(s => s.id === seasonId).map(_.lock).update("open"))
  }

  override def checkAndSetLock(seasonId: Long): Boolean = {
    // Note this function blocks
    val run: Future[Int] = db.run(repo.seasons.filter(s => s.id === seasonId && s.lock =!= "lock" && s.lock =!= "update").map(_.lock).update("update"))
    val map: Future[Boolean] = run.map(n => n == 1)

    Await.result(map, Duration(15, TimeUnit.SECONDS))
  }

  override def listSeasons: Future[List[Season]] = db.run(repo.seasons.to[List].result)


}
