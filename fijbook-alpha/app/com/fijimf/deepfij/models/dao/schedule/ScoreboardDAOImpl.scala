package com.fijimf.deepfij.models.dao.schedule

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Game, Result, ScheduleRepository}
import controllers.{GameMapping, MappedGame, MappedGameAndResult, UnmappedGame}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


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
    db.run(stageGameResult(g, r).transactionally)
  }

  override def updateScoreboard(updateData: List[GameMapping], sourceKey: String): Future[(Seq[Long], Seq[Long])] = {
    val mutations = updateData.map {
      case MappedGame(g) => stageGameResult(g,None)
      case MappedGameAndResult(g, r) => stageGameResult(g, Some(r))
      case UnmappedGame(_, _) => DBIO.successful(None)
    }

    val updateAndCleanUp = DBIO.sequence(mutations).flatMap(gameList=>deletes(gameList,sourceKey)).transactionally

    runWithRecover(updateAndCleanUp, backoffStrategy)
  }

  def stageGameResult(game: Game, res: Option[Result]): DBIO[Option[Game]] = {
    (game, res) match {
      case (g, None) => handleGame(g).flatMap(g1 => deleteGameResult(g1).andThen(DBIO.successful(g1)))
      case (g, Some(r)) => handleGame(g).flatMap(g1 => handleResult(g1, r))
    }
  }

  private def deleteGameResult(g1: Option[Game]) = {
    repo.results.filter(_.gameId === g1.map(_.id).getOrElse(0L)).delete
  }

  def deletes(gameList: List[Option[Game]], sourceKey: String): DBIO[(Seq[Long], Seq[Long])] = {
    val knownIds = gameList.flatten.map(_.id)
    for {
      unknownIds <- findUnknownGames(sourceKey, knownIds)
      _ <- repo.results.filter(_.gameId.inSet(unknownIds)).delete
      _ <- repo.games.filter(_.id.inSet(unknownIds)).delete
    } yield {
      (knownIds, unknownIds)
    }
  }

  def findUnknownGames(sourceKey: String, knownIds: List[Long]): DBIO[Seq[Long]] = {
    repo.games.filter(g => {
      g.sourceKey === sourceKey && !g.id.inSet(knownIds)
    }).map(_.id).result
  }

  private def handleGame(g: Game): DBIO[Option[Game]] = {
    findMatchingGame(g).result.headOption.flatMap {
      case Some(gh) if g.isMateriallyDifferent(gh) => createUpdateGameAction(g, gh)
      case Some(gh) if !g.isMateriallyDifferent(gh) => createNoOpGameAction(gh)
      case None => createInsertGameAction(g)
    }
  }

  private def createInsertGameAction(g: Game): DBIO[Option[Game]] = {
    ((repo.games returning repo.games.map(_.id)) += g).flatMap(id => DBIO.successful(Some(g.copy(id = id))))
  }

  private def createNoOpGameAction(g: Game): DBIO[Option[Game]] = {
    DBIO.successful(Some(g))
  }

  private def createUpdateGameAction(g: Game, gh: Game): DBIO[Option[Game]] = {
    val g1 = g.copy(id = gh.id)
    repo.games.filter(_.id === g1.id).update(g1).andThen(DBIO.successful(Some(g1)))
  }

  private def handleResult(og: Option[Game], r: Result): DBIO[Option[Game]] = {
    og match {
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
