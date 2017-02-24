package com.fijimf.deepfij.models.dao.schedule

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait GameDAOImpl extends GameDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository
  import dbConfig.driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE_TIME),
    str => LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(str))
  )
  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate] = MappedColumnType.base[LocalDate, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE),
    str => LocalDate.from(DateTimeFormatter.ISO_DATE.parse(str))
  )

  override def listGames: Future[List[Game]] = db.run(repo.games.to[List].result)
  override def clearGamesByDate(d: LocalDate): Future[Int] = {
    val dateGames = repo.games.filter(g => g.date === d)
    val dateResults = repo.results.filter(_.gameId in dateGames.map(_.id))
    db.run((dateResults.delete andThen dateGames.delete).transactionally)
  }

  override def saveGame(gt: (Game, Option[Result])): Future[Long] = {
    val (game, optResult) = gt

    optResult match {
      case Some(result) =>
        db.run((for (
          qid <- (repo.games returning repo.games.map(_.id)) += game;
          _ <- repo.results returning repo.results.map(_.id) += result.copy(gameId = qid)
        ) yield qid).transactionally)
      case None =>
        db.run((for (
          qid <- (repo.games returning repo.games.map(_.id)) += game
        ) yield qid).transactionally)
    }
  }

  override def gamesByDate(ds: List[LocalDate]): Future[List[(Game, Option[Result])]] =
    db.run(repo.gameResults.filter(_._1.date inSet ds).to[List].result)

  override def gamesBySource(sourceKey: String): Future[List[(Game, Option[Result])]] =
    db.run(repo.gameResults.filter(_._1.sourceKey === sourceKey).to[List].result)


  // Quote

}
