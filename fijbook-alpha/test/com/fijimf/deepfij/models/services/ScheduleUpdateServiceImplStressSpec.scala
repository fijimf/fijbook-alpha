package com.fijimf.deepfij.models.services

import java.time.LocalDateTime

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import controllers.{GameMapping, MappedGame, MappedGameAndResult}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.Logger
import play.api.test.{FakeApplication, WithApplication}
import testhelpers.Injector

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Random, Success}


class ScheduleUpdateServiceImplStressSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin {

  import scala.concurrent.ExecutionContext.Implicits.global

  val log = Logger(this.getClass)
  val dao = Injector.inject[ScheduleDAO]
  val svc = Injector.inject[ScheduleUpdateService]

  val rng = new Random(0L)
  val today = LocalDateTime.now

  override def beforeEach() = {
    Await.result(repo.createSchema(), testDbTimeout)
  }

  override def afterEach() = {
    Await.result(repo.dropSchema(), testDbTimeout)
  }

  private def assertEmptySchedule(tag: String) = {
    assertTableSizes(0, 0, 0, 0, tag)
  }

  private def assertTableSizes(numSeasons: Int, numTeams: Int, numGames: Int, numResults: Int, tag: String) = {
    assert(Await.result(dao.listTeams, testDbTimeout).size == numTeams, s"Team table size incorrect $tag")
    assert(Await.result(dao.listGames, testDbTimeout).size == numGames, s"Game table size incorrect $tag")
    assert(Await.result(dao.listResults, testDbTimeout).size == numResults, s"Result table size incorrect $tag")
    assert(Await.result(dao.listSeasons, testDbTimeout).size == numSeasons, s"Season table size incorrect $tag")
  }

  private def createNewSeason = {
    val season = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), testDbTimeout)
    val seasons = Await.result(dao.listSeasons, testDbTimeout)
    assert(seasons.size == 1)
    assert(season == seasons.head)
    season
  }

  def createNTeams(numTeams: Int): List[Team] = {
    val teams = 1.to(numTeams).map(n => {
      val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      dao.saveTeam(t)
    }).toList
    val result: List[Team] = Await.result(Future.sequence(teams), testDbTimeout)
    val list = Await.result(dao.listTeams, testDbTimeout)
    assert(result.size == list.size)
    assert(result.sortBy(_.key) == list.sortBy(_.key))
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
    "be empty initially" in new WithApplication(FakeApplication()) {
      assertEmptySchedule(" brand new database")
    }

    "save games" in new WithApplication(FakeApplication()) {
      assertEmptySchedule(" new database before saving game data")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 350)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 120, numResultDays = 0, gamesPerDay = 30, teams, season)
      Await.result(svc.updateDb( keys, gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 350, gameMapping.size, 0, " after saving games, in save games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }

    "re-save games (update with no changes)" in new WithApplication(FakeApplication()) {
      assertEmptySchedule(" new database before saving, before updating")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 350)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 120, numResultDays = 0, gamesPerDay = 30, teams, season)

      Await.result(svc.updateDb(keys,  gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 350, gameMapping.size, 0, " after saving games, before updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)

      val schedule = Await.result(dao.loadLatestSchedule(), testDbTimeout).get

      Await.result(svc.updateDb(keys, gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 350, gameMapping.size, 0, " after saving games, after updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }

    "re-save games (removeGames)" in new WithApplication(FakeApplication()) {
      assertEmptySchedule(" new database before saving, before updating")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 350)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 120, numResultDays = 0, gamesPerDay = 30, teams, season)

      Await.result(svc.updateDb(keys,  gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 350, gameMapping.size, 0, " after saving games, before updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)

      val schedule = Await.result(dao.loadLatestSchedule(), testDbTimeout).get
      val (lessGames, _) = gameMapping.splitAt(gameMapping.size / 2)
      Await.result(svc.updateDb(keys, lessGames).andThen {
        case Success(_) => assertTableSizes(1, 350, lessGames.size, 0, " after saving games, after updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }

    "re-save games (add games)" in new WithApplication(FakeApplication()) {
      assertEmptySchedule(" new database before saving, before updating")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 350)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 120, numResultDays = 0, gamesPerDay = 30, teams, season)

      Await.result(svc.updateDb(keys,  gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 350, gameMapping.size, 0, " after saving games, before updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)

      val schedule = Await.result(dao.loadLatestSchedule(), testDbTimeout).get

      val (moreKeys,moreGames) = createNDaysGamesMDaysResults(numGameDays = 10, numResultDays = 0, gamesPerDay = 1, teams, season)

      Await.result(svc.updateDb(keys++moreKeys, gameMapping ++ moreGames).andThen {
        case Success(_) => assertTableSizes(1, 350, gameMapping.size + moreGames.size, 0, " after saving games, after updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }


    "save games & results" in new WithApplication(FakeApplication()) {
      assertEmptySchedule(" new database before saving game data")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 350)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 120, numResultDays = 60, gamesPerDay = 30, teams, season)
      Await.result(svc.updateDb(keys,  gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 350, 120 * 30, 60 * 30, " after saving games, in save games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)

    }

    "re-save games & results (update with no changes)" in new WithApplication(FakeApplication()) {
      assertEmptySchedule(" new database before saving, before updating")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 350)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 120, numResultDays = 60, gamesPerDay = 30, teams, season)

      Await.result(svc.updateDb(keys,  gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 350, 120 * 30, 60 * 30, " after saving games, before updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)

      val schedule = Await.result(dao.loadLatestSchedule(), testDbTimeout).get

      Await.result(svc.updateDb(keys, gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 350, 120 * 30, 60 * 30, " after saving games, after updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }

    "re-save games & results (remove games)" in new WithApplication(FakeApplication()) {
      assertEmptySchedule(" new database before saving, before updating")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 350)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 120, numResultDays = 60, gamesPerDay = 30, teams, season)

      Await.result(svc.updateDb(keys, gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 350, 120 * 30, 60 * 30, " after saving games, before updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)

      val schedule = Await.result(dao.loadLatestSchedule(), testDbTimeout).get
      val lessGames = gameMapping.takeRight(gameMapping.size / 2)

      Await.result(svc.updateDb(keys, lessGames).andThen {
        case Success(_) => assertTableSizes(1, 350, 60 * 30, 60 * 30, " after saving games, after updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }

    "re-save games & results (remove results)" in new WithApplication(FakeApplication()) {
      assertEmptySchedule(" new database before saving, before updating")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 350)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 120, numResultDays = 60, gamesPerDay = 30, teams, season)

      Await.result(svc.updateDb(keys, gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 350, 120 * 30, 60 * 30, " after saving games, before updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)

      val schedule = Await.result(dao.loadLatestSchedule(), testDbTimeout).get
      val removeResults = gameMapping.map {
        case MappedGameAndResult(g, _) => MappedGame(g)
        case gm: GameMapping => gm
      }

      Await.result(svc.updateDb(keys, removeResults).andThen {
        case Success(_) => assertTableSizes(1, 350, 120 * 30, 0, " after saving games, after updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }

    "re-save games & results (add results)" in new WithApplication(FakeApplication()) {
      assertEmptySchedule(" new database before saving, before updating")
      val season = createNewSeason
      val teams = createNTeams(numTeams = 350)
      val (keys, gameMapping) = createNDaysGamesMDaysResults(numGameDays = 120, numResultDays = 60, gamesPerDay = 30, teams, season)

      Await.result(svc.updateDb(keys,  gameMapping).andThen {
        case Success(_) => assertTableSizes(1, 350, 120 * 30, 60 * 30, " after saving games, before updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)

      val schedule = Await.result(dao.loadLatestSchedule(), testDbTimeout).get

      def res() = Result(0L, 0L, 50 + rng.nextInt(50), 50 + rng.nextInt(50), 2, LocalDateTime.now(), "me")

      val addResults = gameMapping.map {
        case MappedGame(g) => MappedGameAndResult(g, res())
        case gm: GameMapping => gm
      }


      Await.result(svc.updateDb(keys, addResults).andThen {
        case Success(_) => assertTableSizes(1, 350, 120 * 30, 120 * 30, " after saving games, after updating in update games")
        case Failure(ex) => fail("svc.updateDb threw an unexpected exception")
      }, testDbTimeout)
    }


    "delete games & results" in new WithApplication(FakeApplication()) {
      assertEmptySchedule(" new database delete games & results ")

      val s = createNewSeason
      val lst = createNTeams(10)

      val today = LocalDateTime.now

      private val gameMapping = 0.to(3).flatMap(n => {
        val split: (List[Team], List[Team]) = rng.shuffle(lst).splitAt(5)
        val pairs: List[(Team, Team)] = split._1.zip(split._2).take(4)
        pairs.map(tup => {
          val game = quikGame(s,tup, today.plusDays(n))
          if (rng.nextBoolean()) {
            MappedGame(game)
          }
          else {
            MappedGameAndResult(game, Result(0L, 0L, 50 + rng.nextInt(50), 50 + rng.nextInt(50), 2, LocalDateTime.now(), "me"))
          }
        })
      }).toList

      Await.ready(svc.updateDb(0.to(3).toList.map(today.plusDays(_).toLocalDate.toString), gameMapping), testDbTimeout).onComplete {
        case Success(_) => {
          assert(Await.result(dao.listTeams, testDbTimeout).size == 10)
          assert(Await.result(dao.listGames, testDbTimeout).size == 16)
          assert(Await.result(dao.listResults, testDbTimeout).nonEmpty)
          assert(Await.result(dao.listSeasons, testDbTimeout).size == 1)
        }
        case Failure(ex) => {
          fail("svc.updateDb threw an unexpected exception")
        }
      }
      private val keys = gameMapping.map(_.sourceKey).toSet.toList

      private val schedule = Await.result(dao.loadLatestSchedule(), testDbTimeout).get
      Await.result(svc.updateDb(keys, gameMapping.drop(8)), testDbTimeout)

      assert(Await.result(dao.listTeams, testDbTimeout).size == 10)
      assert(Await.result(dao.listGames, testDbTimeout).size == 8)
      assert(Await.result(dao.listResults, testDbTimeout).nonEmpty)
      assert(Await.result(dao.listSeasons, testDbTimeout).size == 1)
    }


    "modify games & results" in new WithApplication(FakeApplication()) {
      assertEmptySchedule("brand new schedule, before modify games and results")

      val s = createNewSeason
      val lst = createNTeams(350)
      val keys = 0.to(120).toList.map(today.plusDays(_).toLocalDate.toString)

      private val gameMapping = 0.to(120).flatMap(n => {
        val split: (List[Team], List[Team]) = rng.shuffle(lst).splitAt(175)
        val pairs: List[(Team, Team)] = split._1.zip(split._2).take(20 + rng.nextInt(30))
        pairs.map(tup => {
          val game = Game(0L, s.id, tup._1.id, tup._2.id, today.plusDays(n).toLocalDate, today.plusDays(n), None, isNeutralSite = false, None, None, None, today.plusDays(n).toLocalDate.toString, LocalDateTime.now(), "me")
          if (rng.nextBoolean()) {
            MappedGame(game)
          } else {
            MappedGameAndResult(game, Result(0L, 0L, 50 + rng.nextInt(50), 50 + rng.nextInt(50), 2, LocalDateTime.now(), "me"))
          }
        })
      }).toList

      Await.result(svc.updateDb(keys, gameMapping).andThen {
        case Success(_) => {
          assert(Await.result(dao.listTeams, testDbTimeout).size == 350)
          assert(Await.result(dao.listGames, testDbTimeout).size == gameMapping.size)
          assert(Await.result(dao.listResults, testDbTimeout).nonEmpty)
          assert(Await.result(dao.listSeasons, testDbTimeout).size == 1)
        }
        case Failure(ex) => {
          fail("svc.updateDb threw an unexpected exception")
        }
      }, testDbTimeout)

      private val schedule = Await.result(dao.loadLatestSchedule(), testDbTimeout).get
      private val gameMapping2 = gameMapping.map {
        case MappedGameAndResult(g, _) => MappedGame(g)
        case x: MappedGame => x
        case _ => fail("Unexpected game mapping")
      }

      Await.result(svc.updateDb(keys, gameMapping2).andThen {
        case Success(_) => {
          assert(Await.result(dao.listTeams, testDbTimeout).size == 350)
          assert(Await.result(dao.listGames, testDbTimeout).size == gameMapping.size)
          val result = Await.result(dao.listResults, testDbTimeout)
          assert(result.isEmpty)
          assert(Await.result(dao.listSeasons, testDbTimeout).size == 1)
        }
        case Failure(ex) => {
          fail("svc.updateDb threw an unexpected exception")
        }
      }, testDbTimeout)

    }

  }
}
