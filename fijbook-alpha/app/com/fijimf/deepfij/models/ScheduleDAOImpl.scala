package com.fijimf.deepfij.models

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.dbio.Effect.Write

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class ScheduleDAOImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, protected val repo: ScheduleRepository) extends ScheduleDAO with DAOSlick {
  val log = Logger(getClass)

  import dbConfig.driver.api._


  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE_TIME),
    str => LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(str))
  )
  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate] = MappedColumnType.base[LocalDate, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE),
    str => LocalDate.from(DateTimeFormatter.ISO_DATE.parse(str))
  )

  //**** Team
  override def listTeams: Future[List[Team]] = db.run(repo.teams.to[List].result)

  override def findTeamByKey(key: String): Future[Option[Team]] = db.run(repo.teams.filter(team => team.key === key).result.headOption)

  override def findTeamById(id: Long): Future[Option[Team]] = db.run(repo.teams.filter(team => team.id === id).result.headOption)

  override def saveTeam(team: Team): Future[Int] = {
    db.run(repo.teams.filter(t => t.key === team.key).result.flatMap(ts =>
      ts.headOption match {
        case Some(t) =>
          if (!t.lockRecord) repo.teams.insertOrUpdate(team.copy(id = t.id)) else repo.teams.filter(z => z.id === -1L).update(team)
        case None =>
          repo.teams.insertOrUpdate(team)
      }
    ).transactionally)
  }

  override def deleteTeam(id: Long): Future[Int] = db.run(repo.teams.filter(team => team.id === id).delete)

  override def unlockTeam(key: String): Future[Int] = db.run(repo.teams.filter(t => t.key === key).map(_.lockRecord).update(false))

  override def lockTeam(key: String): Future[Int] = db.run(repo.teams.filter(t => t.key === key).map(_.lockRecord).update(true))

  //******* Season

  override def saveSeason(s: Season): Future[Int] = db.run(repo.seasons.insertOrUpdate(s))

  override def findSeasonById(id: Long): Future[Option[Season]] = {
    val q: Query[repo.SeasonsTable, Season, Seq] = repo.seasons.filter(season => season.id === id)
    db.run(q.result.headOption)
  }

  override def listSeasons: Future[List[Season]] = db.run(repo.seasons.to[List].result)

  override def unlockSeason(seasonId: Long): Future[Int] = {
    log.info("Unlocking season " + seasonId)
    db.run(repo.seasons.filter(s => s.id === seasonId).map(_.lock).update("open"))
  }

  override def checkAndSetLock(seasonId: Long): Boolean = {
    // Note this function blocks
    val run: Future[Int] = db.run(repo.seasons.filter(s => s.id === seasonId && s.lock =!= "lock" && s.lock =!= "update").map(_.lock).update("update"))
    val map: Future[Boolean] = run.map(n => n == 1)
    log.info("Locking season " + seasonId + " for update")
    Await.result(map, Duration(15, TimeUnit.SECONDS))
  }

  //******* Game

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


  // Quote

  override def listQuotes: Future[List[Quote]] = db.run(repo.quotes.to[List].result)

  override def findQuoteById(id: Long): Future[Option[Quote]] = db.run(repo.quotes.filter(_.id === id).result.headOption)
  override def findQuoteByKey(key:Option[String]): Future[List[Quote]] = db.run(repo.quotes.filter(_.key === key).to[List].result)

  override def saveQuote(q: Quote): Future[Int] = db.run(repo.quotes.insertOrUpdate(q))

  override def deleteQuote(id: Long): Future[Int] = db.run(repo.quotes.filter(_.id === id).delete)

  // Conference

  override def findSeasonByYear(year: Int) = db.run(repo.seasons.filter(_.year === year).result.headOption)

  override def deleteAliases(): Future[Int] = db.run(repo.aliases.delete)

  override def saveConferenceMap(cm: ConferenceMap) = db.run(repo.conferenceMaps.insertOrUpdate(cm))

  override def findConferenceById(id: Long): Future[Option[Conference]] = db.run(repo.conferences.filter(_.id === id).result.headOption)

  override def deleteConference(id: Long): Future[Int] = db.run(repo.conferences.filter(_.id === id).delete)

  override def listConferences: Future[List[Conference]] = db.run(repo.conferences.to[List].result)

  override def saveConference(c: Conference): Future[Int] = db.run(repo.conferences.insertOrUpdate(c))


  // Schedule

  def loadSchedule(s: Season): Future[Schedule] = {
    val fTeams: Future[List[Team]] = db.run(repo.teams.to[List].result)
    val fConferences: Future[List[Conference]] = db.run(repo.conferences.to[List].result)
    val fConferenceMaps: Future[List[ConferenceMap]] = db.run(repo.conferenceMaps.filter(_.seasonId === s.id).to[List].result)
    val fResults: Future[List[(Game, Option[Result])]] = db.run(repo.gameResults.filter(_._1.seasonId === s.id).to[List].result)
    for (
      teams <- fTeams;
      conferences <- fConferences;
      conferenceMap <- fConferenceMaps;
      results <- fResults
    ) yield {
      Schedule(s, teams, conferences, conferenceMap, results)
    }
  }

  override def loadSchedules(): Future[List[Schedule]] = {
    db.run(repo.seasons.to[List].result).flatMap(seasons =>
      Future.sequence(seasons.map(season => loadSchedule(season)))
    )
  }

  // Aliases

  override def listAliases: Future[List[Alias]] = db.run(repo.aliases.to[List].result)

  override def deleteStatValues(dates: List[LocalDate], models: List[String]): Future[Unit] = {
    val map: List[DBIOAction[Int, NoStream, Write]]=
    for (m <- models; d <- dates) yield {
      repo.statValues.filter(sv => sv.date === d && sv.modelKey === m).delete
    }
    db.run(DBIO.seq(map: _*).transactionally)
  }

  override def findAliasById(id: Long): Future[Option[Alias]] = db.run(repo.aliases.filter(_.id === id).result.headOption)

  override def saveAlias(a: Alias): Future[Int] = db.run(repo.aliases.insertOrUpdate(a))

  override def saveStatValues(stats: List[StatValue]): Future[Option[Int]] = {
    db.run(repo.statValues.forceInsertAll(stats))
  }

  override def deleteAlias(id: Long): Future[Int] = db.run(repo.aliases.filter(_.id === id).delete)

  override def loadStatValues(statKey: String, modelKey: String):Future[List[StatValue]]= db.run(repo.statValues.filter(sv=>sv.modelKey === modelKey && sv.statKey===statKey ).to[List].result)
}