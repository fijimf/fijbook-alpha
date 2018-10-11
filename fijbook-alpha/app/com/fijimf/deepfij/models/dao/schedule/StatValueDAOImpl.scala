package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
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

  override def saveXStats(xstats: List[XStat]): Future[List[XStat]] = {
    db.run(DBIO.sequence(xstats.map(upsertByCompoundKey)).transactionally)
  }

  override def saveXStat(xstat: XStat): Future[XStat] = {
    db.run(upsertByCompoundKey(xstat).transactionally)
  }

  def upsertByCompoundKey(xstat: XStat): DBIO[XStat] = {
    for {
      rowsAffected <- repo.xstats.filter(x => x.date === xstat.date && x.key === xstat.key && x.teamId === xstat.teamId).update(xstat)
      _ <- rowsAffected match {
        case 0 => repo.xstats += xstat
        case 1 => DBIO.successful(1)
        case n => DBIO.failed(new RuntimeException(
          s"Expected 0 or 1 change, not $n for ${xstat.date}, ${xstat.key}, ${xstat.teamId}"))
      }
      result <- repo.xstats.filter(x => x.date === xstat.date && x.key === xstat.key && x.teamId === xstat.teamId).result.head
    } yield result
  }
}
