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

  override def listStatValues: Future[List[StatValue]] = db.run(repo.statValues.to[List].result)

  override def deleteStatValues(dates: List[LocalDate], models: List[String]): Future[Int] = {
    db.run(statValuesDeleteActions(models,dates))
  }

  override def saveStatValues(dates: List[LocalDate], models: List[String], stats: List[StatValue]): Future[Seq[Long]] = {
    val key = s"[${models.mkString(", ")}] x [${dates.head} .. ${dates.last}] "
    val start = System.currentTimeMillis()
    log.info(s"Saving stat batch for $key (${stats.size} rows)")

    val deletes = statValuesDeleteActions(models,dates)
    val inserts = statValuesInsertActions(stats)
    val res = runWithRecover(deletes.flatMap(_ => inserts), backoffStrategy)
    res.onComplete {
      case Success(ids) =>
        val dur = System.currentTimeMillis() - start
        log.info(s"Completed saving $key in $dur ms. (${1000 * stats.size / dur} rows/sec)")
        ids
      case Failure(ex) =>
        log.error(s"Saving $key failed with error: ${ex.getMessage}", ex)
        Seq.empty[Long]
    }
    res
  }

  private def statValuesDeleteActions(models: List[String], dates: List[LocalDate]): DBIO[Int] = {
    repo.statValues.filter(sv => sv.date.inSetBind(dates) && sv.modelKey.inSetBind(models)).delete
  }

  private def statValuesInsertActions(statValues: List[StatValue]) = {
    (repo.statValues returning repo.statValues.map(_.id)) ++= statValues
  }

  override def loadStatValues(statKey: String, modelKey: String): Future[List[StatValue]] = db.run(repo.statValues.filter(sv => sv.modelKey === modelKey && sv.statKey === statKey).to[List].result)

  override def loadStatValues(modelKey: String): Future[List[StatValue]] = db.run(repo.statValues.filter(sv => sv.modelKey === modelKey).to[List].result)

  override def loadStatValues(modelKey: String, from:LocalDate, to:LocalDate): Future[List[StatValue]] = {
    db.run(repo.statValues.filter(sv => sv.modelKey === modelKey && sv.date>=from && sv.date<=to).to[List].result)
  }

  override def findXStatsTimeSeries(seasonId:Long, teamId:Long, modelKey:String):Future[List[XStat]] = {
    db.run(repo.xstats.filter(x=>x.seasonId===seasonId && x.teamId === teamId && x.key === modelKey ).sortBy(_.date).to[List].result)
  }

  override def findXStatsSnapshot(seasonId:Long, date:LocalDate, modelKey:String):Future[List[XStat]] = {
    db.run(repo.xstats.filter(x=>x.seasonId===seasonId && x.date === date && x.key === modelKey ).to[List].result)
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
}
