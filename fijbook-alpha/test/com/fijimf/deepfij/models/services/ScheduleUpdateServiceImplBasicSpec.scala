package com.fijimf.deepfij.models.services

import java.time.LocalDateTime

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.schedule.services.{ScheduleUpdateService, ScheduleUpdateServiceImpl}
import controllers.{GameMapping, MappedGame, MappedGameAndResult}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Logger
import play.api.test.WithApplication
import testhelpers.Injector

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Random, Success}


class ScheduleUpdateServiceImplBasicSpec extends PlaySpec with GuiceOneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin {

  import scala.concurrent.ExecutionContext.Implicits.global

  val log = Logger(this.getClass)
  val dao = Injector.inject[ScheduleDAO]
  val svc = Injector.inject[ScheduleUpdateService].asInstanceOf[ScheduleUpdateServiceImpl]

  val rng = new Random(0L)
  val today = LocalDateTime.now

  private def assertEmptySchedule(tag: String) = {
    assertTableSizes(0, 0, 0, 0, tag)
  }

  private def assertTableSizes(numSeasons: Int, numTeams: Int, numGames: Int, numResults: Int, tag: String) = {
    assert(Await.result(dao.listTeams, testDbTimeout).size === numTeams, s"Team table size incorrect $tag")
    assert(Await.result(dao.listGames, testDbTimeout).size === numGames, s"Game table size incorrect $tag")
    assert(Await.result(dao.listResults, testDbTimeout).size === numResults, s"Result table size incorrect $tag")
    assert(Await.result(dao.listSeasons, testDbTimeout).size === numSeasons, s"Season table size incorrect $tag")
  }

  private def createNewSeason = {
    val season = Await.result(dao.saveSeason(Season(0L, 2017)), testDbTimeout)
    val seasons = Await.result(dao.listSeasons, testDbTimeout)
    assert(seasons.size === 1)
    assert(season === seasons.head)
    season
  }

  def createNTeams(numTeams: Int): List[Team] = {
    val teams = 1.to(numTeams).map(n => {
      val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      dao.saveTeam(t)
    }).toList
    val result: List[Team] = Await.result(Future.sequence(teams), testDbTimeout)
    val list = Await.result(dao.listTeams, testDbTimeout)
    assert(result.size === list.size)
    assert(result.sortBy(_.key) === list.sortBy(_.key))
    list
  }

  def createNDaysGamesMDaysResults(numGameDays: Int, numResultDays: Int, gamesPerDay: Int, teams: List[Team], season: Season): (List[String], List[GameMapping]) = {
    require(numResultDays <= numGameDays)
    require(gamesPerDay > 0 && gamesPerDay < teams.size / 2)
    require(teams.nonEmpty)
    val gameTimes: List[LocalDateTime] = 0.until(numGameDays).map(today.plusDays(_)).toList
    val gs: List[Game] = gameTimes.flatMap(n => {
      val split: (List[Team], List[Team]) = rng.shuffle(teams).splitAt(math.floor(teams.size / 2.0).toInt)
      val pairs: List[(Team, Team)] = split._1.zip(split._2).take(gamesPerDay)
      pairs.map(tup => {
        quikGame(season, tup, n)
      })
    })

    def res() = Result(0L, 0L, 50 + rng.nextInt(50), 50 + rng.nextInt(50), 2, LocalDateTime.now(), "me")

    val (gr, g) = gs.splitAt(numResultDays * gamesPerDay)
    (gameTimes.map(_.toLocalDate.toString), g.map(MappedGame) ++ gr.map(MappedGameAndResult(_, res())))
  }

  private def quikGame(season: Season, tup: (Team, Team), gameTime: LocalDateTime) = {
    Game(0L, season.id, tup._1.id, tup._2.id, gameTime.toLocalDate, gameTime, None, isNeutralSite = false, None, None, None, gameTime.toLocalDate.toString, LocalDateTime.now(), "me")
  }

  "Games & results " should {

    "save 1 days games" in new WithApplication() {
      assertEmptySchedule(" new database before saving game data")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 6)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 1, numResultDays = 0, gamesPerDay = 2, teams, season)
      Await.result(svc.updateDb(keys, gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 6, 2, 0, " after saving games, in save games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }
    "save 1 days games, then erase them" in new WithApplication() {
      assertEmptySchedule(" new database before saving game data")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 6)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 1, numResultDays = 0, gamesPerDay = 2, teams, season)
      Await.result(svc.updateDb(keys, gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 6, 2, 0, " after saving games, in save games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
      Await.result(svc.updateDb(keys, List.empty[GameMapping]).andThen {
        case Success(_) => assertTableSizes(1, 6, 0, 0, " after saving games, in save games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }
    "save 1 days games, then update them" in new WithApplication() {
      assertEmptySchedule(" new database before saving game data")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 6)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 1, numResultDays = 0, gamesPerDay = 2, teams, season)
      Await.result(svc.updateDb(keys,gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 6, 2, 0, " after saving games, in save games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
      val gameIds = Await.result(dao.listGames, testDbTimeout).map(_.id).toSet
      private val gameMappings2 = gameMapping.map {
        case MappedGame(g) => MappedGame(g.copy(location = Some("Your mom's house")))
        case x => x
      }
      Await.result(svc.updateDb(keys, gameMappings2).andThen {
        case Success(_) =>
          assertTableSizes(1, 6, 2, 0, " after saving games, in save games")
          Await.result(dao.listGames, testDbTimeout).foreach(g => {
            assert(gameIds.contains(g.id))
            assert(g.location.contains("Your mom's house"))
          })
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }

    "save 1 days games & results" in new WithApplication() {
      assertEmptySchedule(" new database before saving game data")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 6)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 1, numResultDays = 1, gamesPerDay = 2, teams, season)
      Await.result(svc.updateDb(keys, gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 6, 2, 2, " after saving games, in save games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }

    "save 1 days games & results, then erase both games and results" in new WithApplication() {
      assertEmptySchedule(" new database before saving game data")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 6)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 1, numResultDays = 1, gamesPerDay = 2, teams, season)
      Await.result(svc.updateDb(keys, gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 6, 2, 2, " after saving games, in save games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
      private val k = gameMapping.head.sourceKey
      Await.result(svc.updateDb(List(k), List.empty[GameMapping]).andThen {
        case Success(_) => assertTableSizes(1, 6, 0, 0, " after saving games, in save games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }

    "save 1 days games & results, then erase ONLY results" in new WithApplication() {
      assertEmptySchedule(" new database before saving game data")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 6)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 1, numResultDays = 1, gamesPerDay = 2, teams, season)
      Await.result(svc.updateDb(keys, gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 6, 2, 2, " after saving games, in save games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
      val gameIds = Await.result(dao.listGames, testDbTimeout).map(_.id).toSet
      private val gameMappings2 = gameMapping.map {
        case MappedGameAndResult(g, r) => MappedGame(g)
        case x => x
      }
      Await.result(svc.updateDb(keys, gameMappings2).andThen {
        case Success(_) =>
          assertTableSizes(1, 6, 2, 0, " after saving games, in save games")
          Await.result(dao.listGames, testDbTimeout).foreach(g => {
            assert(gameIds.contains(g.id))
          })
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }

  }
}
