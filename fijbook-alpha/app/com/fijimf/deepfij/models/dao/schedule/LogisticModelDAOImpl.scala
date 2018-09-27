package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait LogisticModelDAOImpl extends LogisticModelDAO with DAOSlick {
  val log: Logger

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import scala.concurrent.ExecutionContext.Implicits.global
  import dbConfig.profile.api._

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime]

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate]

  override def listLogisticModelParameters: Future[List[LogisticModelParameter]] = db.run(repo.logisticModels.to[List].result)

  override def saveLogisticModelParameter(lm: LogisticModelParameter): Future[LogisticModelParameter] = db.run(upsert(lm))

  private def upsert(x: LogisticModelParameter) = {
    (repo.logisticModels returning repo.logisticModels.map(_.id)).insertOrUpdate(x).flatMap {
      case Some(id) => repo.logisticModels.filter(_.id === id).result.head
      case None => DBIO.successful(x)
    }
  }


  override def findLogisticModel(model: String): Future[Map[LocalDate, List[LogisticModelParameter]]] = db.run(repo.logisticModels.filter(lm => lm.modelName === model).to[List].result).map(_.groupBy(_.fittedAsOf))

  override def findLogisticModelDate(model: String, asOf: LocalDate): Future[List[LogisticModelParameter]] = findLogisticModel(model).map(_.getOrElse(asOf,List.empty[LogisticModelParameter]))

  override def findLatestLogisticModel(model: String): Future[List[LogisticModelParameter]] = {
    findLogisticModel(model).map(f=> {
      if (f.isEmpty) {
        List.empty[LogisticModelParameter]
      } else {
        val k = f.keys.maxBy(_.toEpochDay)
        f(k)
      }
    })
  }


  override def deleteLogisticModel(model: String): Future[Int] = db.run(repo.logisticModels.filter(_.modelName === model).delete)

  override def deleteLogisticModelDate(model: String, asOf: LocalDate): Future[Int] = db.run(repo.logisticModels.filter(lm => lm.modelName === model && lm.fittedAsOf === asOf).delete)
}
