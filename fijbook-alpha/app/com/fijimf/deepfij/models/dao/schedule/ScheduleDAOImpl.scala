package com.fijimf.deepfij.models.dao.schedule

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import akka.actor.ActorSystem
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import controllers.{GameMapping, MappedGame, MappedGameAndResult, UnmappedGame}
import javax.inject.Inject
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class ScheduleDAOImpl @Inject()(val dbConfigProvider: DatabaseConfigProvider, val repo: ScheduleRepository, val actorSystem: ActorSystem)(implicit ec: ExecutionContext)
  extends ScheduleDAO with DAOSlick
    with TeamDAOImpl
    with GameDAOImpl
    with ResultDAOImpl
    with QuoteDAOImpl
    with SeasonDAOImpl
    with ConferenceDAOImpl
    with ConferenceMapDAOImpl
    with AliasDAOImpl
    with StatValueDAOImpl
    with UserProfileDAOImpl
    with QuoteVoteDAOImpl
    with FavoriteLinkDAOImpl
    with RssFeedDAOImpl
    with RssItemDAOImpl
    with JobDAOImpl
    with JobRunDAOImpl
    with CalcStatusDAOImpl {

  import dbConfig.profile.api._

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE_TIME),
    str => LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(str))
  )

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate] = MappedColumnType.base[LocalDate, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE),
    str => LocalDate.from(DateTimeFormatter.ISO_DATE.parse(str))
  )

  val log = Logger(this.getClass)

  def loadSchedule(s: Season): Future[Schedule] = {
    val fTeams: Future[List[Team]] = db.run(repo.teams.to[List].result)
    val fConferences: Future[List[Conference]] = db.run(repo.conferences.to[List].result)
    val fConferenceMaps: Future[List[ConferenceMap]] = db.run(repo.conferenceMaps.filter(_.seasonId === s.id).to[List].result)
    val fResults: Future[List[(Game, Option[Result])]] = db.run(repo.gameResults.filter(_._1.seasonId === s.id).to[List].result)
    for {
      teams <- fTeams
      conferences <- fConferences
      conferenceMap <- fConferenceMaps
      results <- fResults

    } yield {
      Schedule(s, teams, conferences, conferenceMap, results)
    }
  }

  override def saveGameResult(g: Game, r: Option[Result]): Future[Option[Game]] = {
    db.run(((g, r) match {
      case (g, None) =>
        handleGame(g).flatMap(g1 => repo.results.filter(_.gameId === g1.map(_.id).getOrElse(0L)).delete.andThen(DBIO.successful(g1)))
      case (g, Some(r)) =>
        handleGame(g).flatMap(g1 => handleResult(g1, r))
    }).transactionally)
  }

  override def updateScoreboard(updateData: List[GameMapping], sourceKey: String): Future[(Seq[Long], Seq[Long])] = {
    val startTime = System.currentTimeMillis()
    val mutations = updateData.map {
      case MappedGame(g) =>
        handleGame(g).flatMap(g1 => repo.results.filter(_.gameId === g1.map(_.id).getOrElse(0L)).delete.andThen(DBIO.successful(g1)))
      case MappedGameAndResult(g, r) =>
        handleGame(g).flatMap(g1 => handleResult(g1, r))
      case UnmappedGame(keys, _) =>
        DBIO.successful(None)
    }
    val updateAndCleanUp = DBIO.sequence(mutations).flatMap(ogs => {
      val goodIds = ogs.flatten.map(_.id)
      repo.games.filter(g => {
        g.sourceKey === sourceKey && !g.id.inSet(goodIds)
      }).map(_.id).result.flatMap(badIds => {
        repo.results.filter(_.gameId.inSet(badIds)).delete.
          andThen(repo.games.filter(_.id.inSet(badIds)).delete).
          andThen(DBIO.successful((goodIds, badIds)))
      })
    }).transactionally

    val f = runWithRecover(updateAndCleanUp, backoffStrategy)

    f.onComplete {
      case Success((upserts, deletes)) =>
        log.info(s"UpdateScoreboard succeeded for $sourceKey in ${System.currentTimeMillis() - startTime} with ${upserts.size} upserts and ${deletes.size} deletes")
      case Failure(thr) =>
        log.error(s"UpdateScoreboard failed for $sourceKey in ${System.currentTimeMillis() - startTime} ms with ${thr.getMessage}", thr)
    }
    f
  }

  private def handleGame(g: Game): DBIO[Option[Game]] = {
    sameGame(g).map(_.id).result.flatMap(
      (longs: Seq[Long]) => {
        longs.headOption match {
          case Some(id) =>
            val g1 = g.copy(id = id)
            repo.games.filter(_.id === g1.id).update(g1).andThen(DBIO.successful(Some(g1)))
          case None => ((repo.games returning repo.games.map(_.id)) += g).flatMap(id => DBIO.successful(Some(g.copy(id = id))))
        }
      }
    )
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

  private def sameGame(g: Game) = {
    repo.games.filter(h => h.date === g.date && h.homeTeamId === g.homeTeamId && h.awayTeamId === g.awayTeamId)
  }

  override def loadSchedules(): Future[List[Schedule]] = {
    val result: DBIO[List[Season]] = repo.seasons.to[List].result
    db.run(result).flatMap(seasons =>
      Future.sequence(seasons.map(season => loadSchedule(season)))
    )
  }

  override def loadSchedule(y: Int): Future[Option[Schedule]] = {
    if (y<0) {
      loadLatestSchedule()
    }
    else {
      val s = System.currentTimeMillis()
      val future = db.run(repo.seasons.filter(_.year === y).result.headOption).flatMap {
        case Some(s) => loadSchedule(s).map(Some(_))
        case None => Future.successful(None)
      }
      future.onComplete {
        case Failure(thr) => log.error(s"loadSchedule for $y failed in ${System.currentTimeMillis() - s} ms. Error was ${thr.getMessage}", thr)
        case _ => log.debug(s"loadSchedule from DB for $y completed in ${System.currentTimeMillis() - s} ms.")
      }
      future
    }
  }

  override def loadLatestSchedule(): Future[Option[Schedule]] = {
    loadSchedules().map(_.sortBy(s => -s.season.year).headOption)
  }

}