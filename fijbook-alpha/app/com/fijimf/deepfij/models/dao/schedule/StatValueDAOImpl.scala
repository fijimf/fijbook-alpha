package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
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

  def saveStatBatch(dates: List[LocalDate], models: List[String], stats: List[StatValue]): Future[Seq[Long]] = {
    val key = s"[${models.mkString(", ")}] x [${dates.head} .. ${dates.last}] "
    val start = System.currentTimeMillis()
    log.info(s"Saving stat batch for $key (${stats.size} rows)")

    val deletes = statValuesDeleteActions(models,dates)
    val inserts = statValuesInsertActions(stats)
    val future = db.run(
      deletes
        .andThen(inserts)
        .transactionally
    )
    future.onComplete((t: Try[_]) => {
      val dur = System.currentTimeMillis() - start
      t match {
        case Success(_) => log.info(s"Completed saving $key in $dur ms. (${1000 * stats.size / dur} rows/sec)")
        case Failure(ex) => log.error(s"Saving $key failed with error: ${ex.getMessage}", ex)
      }
    })
    future
  }


  def statValuesDeleteActions(models: List[String], dates: List[LocalDate]): DBIO[_] = {
    val deleteOps = for {
      m <- models
      d <- dates
    } yield {
      repo.statValues.filter(sv => sv.date === d && sv.modelKey === m).delete
    }
    DBIO.sequence(deleteOps)
  }

  def statValuesInsertActions(statValues: List[StatValue]) = {
    val x = repo.statValues returning repo.statValues.map(_.id)
    x ++= statValues //.flatMap(t1 => DBIO.sequence(t1.map(t2=>repo.statValues.filter(t => t.id === t2).result.head).toList))

  }






  override def loadStatValues(statKey: String, modelKey: String): Future[List[StatValue]] = db.run(repo.statValues.filter(sv => sv.modelKey === modelKey && sv.statKey === statKey).to[List].result)

  override def loadStatValues(modelKey: String): Future[List[StatValue]] = db.run(repo.statValues.filter(sv => sv.modelKey === modelKey).to[List].result)

}
