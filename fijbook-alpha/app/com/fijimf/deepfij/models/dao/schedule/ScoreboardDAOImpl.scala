package com.fijimf.deepfij.models.dao.schedule

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.services.UpdateDbResult
import com.fijimf.deepfij.models.{Game, Result, ScheduleRepository}
import controllers.{GameMapping, MappedGame, MappedGameAndResult, UnmappedGame}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

sealed trait GG {
  def g: Option[Game]

  def op: String
}

case class Insert(g: Option[Game]) extends GG {
  val op = "INSERT"
}

case class Update(g: Option[Game]) extends GG {
  val op = "UPDATE"
}

case class NoOp(g: Option[Game]) extends GG {
  val op = "NOOP"
}



trait ScoreboardDAOImpl extends ScoreboardDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE_TIME),
    str => LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(str))
  )

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate] = MappedColumnType.base[LocalDate, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE),
    str => LocalDate.from(DateTimeFormatter.ISO_DATE.parse(str))
  )

  override def saveGameResult(g: Game, r: Option[Result]): Future[Option[Game]] = {
    db.run(stageGameResult(g, r).map(_.g).transactionally)
  }

  override def updateScoreboard(updateData: List[GameMapping], sourceKey: String): Future[UpdateDbResult] = {
    val mutations = updateData.map {
      case MappedGame(g) => stageGameResult(g,None)
      case MappedGameAndResult(g, r) => stageGameResult(g, Some(r))
      case UnmappedGame(_, _) => DBIO.successful(NoOp(None))
    }

    val updateAndCleanUp = DBIO.sequence(mutations).flatMap(gameList=>deletes(gameList,sourceKey)).transactionally

    runWithRecover(updateAndCleanUp, backoffStrategy)
  }

  def stageGameResult(game: Game, res: Option[Result]): DBIO[GG] = {
    (game, res) match {
      case (g, None) => handleGame(g).flatMap(g1 => deleteGameResult(g1.g).andThen(DBIO.successful(g1)))
      case (g, Some(r)) => handleGame(g).flatMap(g1 => handleResult(g1, r))
    }
  }

  private def deleteGameResult(g1: Option[Game]) = {
    repo.results.filter(_.gameId === g1.map(_.id).getOrElse(0L)).delete
  }

  def deletes(gameList: List[GG], sourceKey: String): DBIO[UpdateDbResult] = {
    val knownIds = gameList.flatMap(_.g).map(_.id)
    for {
      unknownIds <- findUnknownGames(sourceKey, knownIds)
      _ <- repo.results.filter(_.gameId.inSet(unknownIds)).delete
      _ <- repo.games.filter(_.id.inSet(unknownIds)).delete
    } yield {
      val opMap: Map[String, List[Long]] = gameList.groupBy(_.op).mapValues(_.flatMap(_.g).map(_.id))
      println(s"$sourceKey    $opMap")
      UpdateDbResult(
        sourceKey,
        opMap.getOrElse("INSERT", Seq.empty[Long]),
        opMap.getOrElse("UPDATE", Seq.empty[Long]),
        opMap.getOrElse("NOOP", Seq.empty[Long]),
        unknownIds
      )
    }
  }

  def findUnknownGames(sourceKey: String, knownIds: List[Long]): DBIO[Seq[Long]] = {
    repo.games.filter(g => {
      g.sourceKey === sourceKey && !g.id.inSet(knownIds)
    }).map(_.id).result
  }

  private def handleGame(g: Game): DBIO[GG] = {
    findMatchingGame(g).result.headOption.flatMap {
      case Some(gh) if g.isMateriallyDifferent(gh) => createUpdateGameAction(g, gh)
      case Some(gh) if !g.isMateriallyDifferent(gh) => createNoOpGameAction(gh)
      case None => createInsertGameAction(g)
    }
  }

  private def createInsertGameAction(g: Game): DBIO[GG] = {
    ((repo.games returning repo.games.map(_.id)) += g).flatMap(id => DBIO.successful(Insert(Some(g.copy(id = id)))))
  }

  private def createNoOpGameAction(g: Game): DBIO[GG] = {
    DBIO.successful(NoOp(Some(g)))
  }

  private def createUpdateGameAction(g: Game, gh: Game): DBIO[GG] = {
    val g1 = g.copy(id = gh.id)
    repo.games.filter(_.id === g1.id).update(g1).andThen(DBIO.successful(Update(Some(g1))))
  }

  private def handleResult(og: GG, r: Result): DBIO[GG] = {
    og.g match {
      case Some(g) =>
        val r0 = r.copy(gameId = g.id)
        repo.results.filter(_.gameId === g.id).map(_.id).result.flatMap(
          (longs: Seq[Long]) => {
            val r1 = r0.copy(id = longs.headOption.getOrElse(0))
            repo.results.insertOrUpdate(r1).andThen(DBIO.successful(og))
          }
        )
      case None => DBIO.successful(og)
    }
  }

  private def findMatchingGame(g: Game) = {
    repo.games.filter(h => h.date === g.date && h.homeTeamId === g.homeTeamId && h.awayTeamId === g.awayTeamId)
  }
}
