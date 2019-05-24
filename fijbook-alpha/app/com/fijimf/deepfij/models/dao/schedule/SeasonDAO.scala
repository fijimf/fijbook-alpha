package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{ScheduleRepository, Season}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait SeasonDAO extends DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository
  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

   def saveSeason(s: Season): Future[Season] = {
  saveSeasons(List(s)).map(_.head)
  }

   def saveSeasons(seasons: List[Season]): Future[List[Season]] = {
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

   def findSeasonById(id: Long): Future[Option[Season]] = {
    val q = repo.seasons.filter(season => season.id === id)
    db.run(q.result.headOption)
  }

   def findSeasonByYear(year: Int): Future[Option[Season]] = {
    val q = repo.seasons.filter(season => season.year === year)
    db.run(q.result.headOption)
  }

   def deleteSeason(id: Long): Future[Int] = db.run(repo.seasons.filter(season => season.id === id).delete)

   def listSeasons: Future[List[Season]] = db.run(repo.seasons.to[List].result)


}
