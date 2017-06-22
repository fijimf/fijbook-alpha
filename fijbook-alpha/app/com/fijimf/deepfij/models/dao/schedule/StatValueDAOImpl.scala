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

  override def deleteStatValues(dates: List[LocalDate], models: List[String]): Future[Int] = {
    db.run(statValuesDeleteActions(models,dates))
  }

  override def saveStatValues(dates: List[LocalDate], models: List[String], stats: List[StatValue]): Future[Seq[Long]] = {
    val key = s"[${models.mkString(", ")}] x [${dates.head} .. ${dates.last}] "
    val start = System.currentTimeMillis()
    log.debug(s"Saving stat batch for $key (${stats.size} rows)")

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
        case Success(_) => log.debug(s"Completed saving $key in $dur ms. (${1000 * stats.size / dur} rows/sec)")
        case Failure(ex) => log.error(s"Saving $key failed with error: ${ex.getMessage}", ex)
      }
    })
    future
  }

  def statValuesDeleteActions(models: List[String], dates: List[LocalDate]): DBIO[Int] = {
    repo.statValues.filter(sv => sv.date.inSetBind(dates) && sv.modelKey.inSetBind(models)).delete
  }

  def statValuesInsertActions(statValues: List[StatValue]) = {
    (repo.statValues returning repo.statValues.map(_.id)) ++= statValues
  }






  override def loadStatValues(statKey: String, modelKey: String): Future[List[StatValue]] = db.run(repo.statValues.filter(sv => sv.modelKey === modelKey && sv.statKey === statKey).to[List].result)

  override def loadStatValues(modelKey: String): Future[List[StatValue]] = db.run(repo.statValues.filter(sv => sv.modelKey === modelKey).to[List].result)

}
