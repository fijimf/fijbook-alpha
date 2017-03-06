package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.Effect.Write

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


trait StatValueDAOImpl extends StatValueDAO with DAOSlick {
  val log: Logger

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime]

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate]


  //Stat values
  override def listStatValues: Future[List[StatValue]] = db.run(repo.statValues.to[List].result)

  override def deleteStatValues(dates: List[LocalDate], models: List[String]): Future[Unit] = {
    val map: List[DBIOAction[Int, NoStream, Write]] =
      for (m <- models; d <- dates) yield {
        repo.statValues.filter(sv => sv.date === d && sv.modelKey === m).delete
      }
    db.run(DBIO.seq(map: _*).transactionally)
  }

  override def saveStatValues(batchSize: Int, dates: List[LocalDate], models: List[String], stats: List[StatValue]): Future[Any] = {
    def batchReq(ds: List[LocalDate]) = saveStatBatch(ds, models, stats.filter(s => ds.contains(s.date)))

    val g = dates.grouped(batchSize).toList
    g.tail.foldLeft(batchReq(g.head)) { case (future: Future[_], dates: List[LocalDate]) => future.flatMap(_ => batchReq(dates)) }
  }

  def saveStatBatch(dates: List[LocalDate], models: List[String], stats: List[StatValue]): Future[Any] = {
    val key = s"[${models.mkString(", ")}] x [${dates.head} .. ${dates.last}] "
    val start = System.currentTimeMillis()
    log.info(s"Saving stat batch for $key (${stats.size} rows)")
    val inserts: DBIOAction[_, NoStream, Write] = repo.statValues ++= stats
    val deletes: List[DBIOAction[_, NoStream, Write]] =
      for (m <- models; d <- dates) yield {
        repo.statValues.filter(sv => sv.date === d && sv.modelKey === m).delete
      }
    val delete = DBIO.seq(deletes: _*)
    val future: Future[Any] = db.run(delete.andThen(inserts).transactionally)
    future.onComplete((t: Try[_]) => {
      val dur = System.currentTimeMillis() - start
      t match {
        case Success(_) => log.info(s"Completed saving $key in $dur ms. (${1000 * stats.size / dur} rows/sec)")
        case Failure(ex) => log.error(s"Saving $key failed with error: ${ex.getMessage}", ex)
      }
    })
    future
  }

  override def loadStatValues(statKey: String, modelKey: String): Future[List[StatValue]] = db.run(repo.statValues.filter(sv => sv.modelKey === modelKey && sv.statKey === statKey).to[List].result)

  override def loadStatValues(modelKey: String): Future[List[StatValue]] = db.run(repo.statValues.filter(sv => sv.modelKey === modelKey).to[List].result)

}
