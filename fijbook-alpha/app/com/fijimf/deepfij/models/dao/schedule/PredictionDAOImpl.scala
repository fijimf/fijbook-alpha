package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import cats.data.OptionT
import cats.implicits.catsStdInstancesForFuture
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.nstats.predictors.PredictionResult
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait PredictionDAOImpl extends PredictionDAO with DAOSlick {
  val log: Logger

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime]

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate]

  override def loadLatestPredictionModel(key: String): OptionT[Future, XPredictionModel] = {
    val notFound = OptionT.pure(-1)
    val default = OptionT.pure(XPredictionModel(key, 0))
    for {
      v <- OptionT(db.run(repo.xpredictionModels.filter(_.key === key).map(_.version).max.result)).orElse(notFound)
      p <- loadPredictionModel(key, v).orElse(default)
    } yield p
  }

  override def loadPredictionModel(key: String, version: Int): OptionT[Future, XPredictionModel] = OptionT(
    db.run(repo.xpredictionModels.filter(pm => pm.key === key && pm.version === version).result.headOption)
  )

  override def loadPredictionModels(key: String): Future[List[XPredictionModel]] = {
    db.run(repo.xpredictionModels.filter(pm => pm.key === key).to[List].result)
  }

  override def savePredictionModel(model: XPredictionModel): OptionT[Future, XPredictionModel] = {
    require(model.version > 0, "Version 0 is reserved and cannot be save")
    require(model.engineData.isDefined,"Cannot save an untrained model")
    OptionT.liftF(db.run(upsert(model)))
  }

  override def updatePredictions(xps: List[XPrediction]): Future[List[XPrediction]] = {
    Future.sequence(xps.groupBy(xp=>(xp.modelId,xp.schedMD5Hash)).map{case ((modelId,md5hash),predictions) => updatePredictions(modelId,md5hash,predictions)}.toList).map(_.flatten)

  }

  override def updatePredictions(modelId: Long, schedHash: String, xps: List[XPrediction]): Future[List[XPrediction]] = {
    db.run(
      (for {
        _ <- repo.xpredictions.filter(x => x.modelId === modelId && x.schedMD5Hash === schedHash).delete
        _ <- repo.xpredictions ++= xps.map(_.copy(modelId = modelId, schedMD5Hash = schedHash))
        ins <- repo.xpredictions.filter(x => x.modelId === modelId && x.schedMD5Hash === schedHash).result
      } yield {
        ins
      }.to[List]).transactionally
    )
  }


  private def upsert(model: XPredictionModel) = {
    (repo.xpredictionModels returning repo.xpredictionModels.map(_.id)).insertOrUpdate(model).flatMap {
      case Some(id) => repo.xpredictionModels.filter(_.id === id).result.head
      case None => DBIO.successful(model)
    }
  }

  override def findXPredictions(modelId: Long): OptionT[Future,List[XPrediction]] = {
    OptionT.liftF(db.run(repo.xpredictions.filter(_.modelId === modelId).to[List].result))
  }

  def findAllPredictions(): Future[Map[Long, List[PredictionResult]]] = {
    db.run(repo.xpredictions.join(repo.games).on(_.gameId === _.id).joinLeft(repo.results).on(_._1.gameId === _.gameId).to[List].result)
      .map(lst => {
        lst.groupBy(_._1._1.modelId).mapValues(_.map(tuple => PredictionResult(tuple._1._2, tuple._2, Some(tuple._1._1))))
      })
  }

}
