package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future
import scala.util.{Failure, Success}


trait GamePredictionDAOImpl extends GamePredictionDAO with DAOSlick {
  val log: Logger

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime]

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate]


  override def listGamePrediction: Future[List[GamePrediction]] = db.run(repo.gamePredictions.to[List].result)

  override def loadGamePredictions(games: List[Game], modelKey: String): Future[List[GamePrediction]] = {
    Future.sequence(games.map(g => {
      db.run(repo.gamePredictions.filter(gp => gp.gameId === g.id && gp.modelKey === modelKey).to[List].result)
    })).map(_.flatten)
  }

  override def saveGamePredictions(gps: List[GamePrediction]): Future[List[Int]] = {
    val saveResults = Future.sequence(gps.map(gp => db.run(repo.gamePredictions.insertOrUpdate(gp))))
    saveResults.onComplete {
      case Success(is) => log.info("Save results: " + is.mkString(","))
      case Failure(ex) => log.error("Failed upserting prediction", ex)
    }
    saveResults
  }

}
