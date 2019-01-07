package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.nstats.SnapshotDbBundle
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future
import scala.util.{Failure, Success}


trait StatValueDAOImpl extends StatValueDAO with DAOSlick {
  val log: Logger

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime]

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate]

  override def findXStatsTimeSeries(seasonId:Long, teamId:Long, modelKey:String):Future[List[XStat]] = {
    db.run(repo.xstats.filter(x=>x.seasonId===seasonId && x.teamId === teamId && x.key === modelKey ).sortBy(_.date).to[List].result).map(_.reverse)
  }

  override def findXStatsSnapshot(seasonId:Long, date:LocalDate, modelKey:String,fallback:Boolean):Future[List[XStat]] = {
    if (fallback){
      for {
        d <- db.run(repo.xstats.filter(x => x.seasonId === seasonId && x.key === modelKey && x.date <= date).map(_.date).max.result)
        snap <- db.run(repo.xstats.filter(x => x.seasonId === seasonId && x.date === d.getOrElse(date) && x.key === modelKey).to[List].result)
      }yield{
        log.info(s"for $seasonId, $date, $modelKey loaded ${snap.size} values.")
        snap
      }
    } else {
      db.run(repo.xstats.filter(x => x.seasonId === seasonId && x.date === date && x.key === modelKey).to[List].result)
    }
  }

  override def findXStatsLatest(seasonId:Long, teamId:Long, modelKey:String):Future[Option[XStat]] = {
    findXStatsTimeSeries(seasonId:Long, teamId:Long, modelKey:String).map(_.headOption)
  }

  override def listXStats: Future[List[XStat]] = db.run(repo.xstats.to[List].result)

  override def saveXStats(xstats: List[XStat]): Future[List[Int]] = {
    db.run(DBIO.sequence(xstats.map(upsertByCompoundKey)).transactionally)
  }

  override def saveXStat(xstat: XStat): Future[Int] = {
    db.run(upsertByCompoundKey(xstat).transactionally)
  }

  override def saveXStatSnapshot(d: LocalDate, k: String, xstats: List[XStat]): Future[Int] = {
    db.run(for {
      _ <- repo.xstats.filter(x => x.date === d && x.key === k).delete
      m <- repo.xstats ++= xstats
    } yield {
      m.getOrElse(-1)
    })
  }

  override def saveBatchedSnapshots(snaps: List[SnapshotDbBundle]): Future[Option[Int]] = {
    log.info(s"'saveBatchedSnapshots' received request to save ${snaps.size} bundles.")
    val deletes = DBIO.sequence(snaps.map(s => (s.k, s.d)).groupBy(_._1).mapValues(_.map(_._2)).map {
      case (key, dates) =>
        repo.xstats.filter(x => x.key === key && x.date.inSet(dates.toSet)).delete
    })
    val inserts = repo.xstats ++= snaps.flatMap(_.xs)
    val returnValue = db.run((for {
      _ <- deletes
      i <- inserts
    } yield {
      i
    }).withPinnedSession)
    returnValue.onComplete{
      case Success(_)=> log.info(s"'saveBatchedSnapshots' completed successfully")
      case Failure(thr)=> log.error(s"'saveBatchedSnapshots' failed!", thr)
    }
    returnValue
  }

  def upsertByCompoundKey(xstat: XStat): DBIO[Int] = {
    for {
      row <- repo.xstats.filter(x => x.date === xstat.date && x.key === xstat.key && x.teamId === xstat.teamId).result.headOption
      m <- row match {
        case None =>
          repo.xstats += xstat
        case Some(x) =>
          repo.xstats.filter(y => y.id === x.id).update(xstat.copy(id = x.id))
          DBIO.successful(1)
      }
      // result <- repo.xstats.filter(x => x.date === xstat.date && x.key === xstat.key && x.teamId === xstat.teamId).result.head
    } yield m
  }

  override def insertSnapshots(snaps: List[SnapshotDbBundle]): Future[Option[Int]] = {
    val stats = snaps.flatMap(_.xs)
    log.info(s"Inserting ${stats.size} statistics from ${snaps.size} bundles.")
    db.run((repo.xstats ++= stats).withPinnedSession)
  }


  override def deleteXStatBySeason(season: Season, key: String): Future[Int] = {
    db.run(repo.xstats.filter(x=> x.seasonId===season.id && x.key===key).delete)
  }
}
