package com.fijimf.deepfij.models.services

import java.time.LocalDateTime

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import controllers.{MappedGame, MappedGameAndResult}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.test.{FakeApplication, WithApplication}
import testhelpers.Injector

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Random


class ScheduleUpdateServiceImplSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val repo = Injector.inject[ScheduleRepository]
  val dao = Injector.inject[ScheduleDAO]
  val svc = Injector.inject[ScheduleUpdateService]

  override def beforeEach() = {
    Await.result(repo.createSchema(), Duration.Inf)
  }

  override def afterEach() = {
    Await.result(repo.dropSchema(), Duration.Inf)
  }


  "Games & results " should {
    import scala.concurrent.ExecutionContext.Implicits.global
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listTeams, Duration.Inf).isEmpty)
      assert(Await.result(dao.listGames, Duration.Inf).isEmpty)
      assert(Await.result(dao.listResults, Duration.Inf).isEmpty)
      assert(Await.result(dao.listSeasons, Duration.Inf).isEmpty)

    }

    "save games" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listTeams, Duration.Inf).isEmpty)
      assert(Await.result(dao.listGames, Duration.Inf).isEmpty)
      assert(Await.result(dao.listResults, Duration.Inf).isEmpty)
      assert(Await.result(dao.listSeasons, Duration.Inf).isEmpty)
      val s = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), Duration.Inf)
      private val teams = 1.to(350).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }).toList
      Await.result(Future.sequence(teams), Duration.Inf)
      val lst = Await.result(dao.listTeams, Duration.Inf)
      val rng = new Random(0L)
      val today = LocalDateTime.now

      private val gameMapping = 0.to(120).flatMap(n => {
        val split: (List[Team], List[Team]) = rng.shuffle(lst).splitAt(175)
        val pairs: List[(Team, Team)] = split._1.zip(split._2).take(20 + rng.nextInt(30))
        pairs.map(tup => MappedGame(Game(0L, s.id, tup._1.id, tup._2.id, today.plusDays(n).toLocalDate, today.plusDays(n), None, false, None, None, None, "", LocalDateTime.now(), "me")))
      }).toList

      svc.updateDb(List.empty[(Game, Option[Result])], gameMapping)

      assert(Await.result(dao.listTeams, Duration.Inf).size == 350)
      assert(Await.result(dao.listGames, Duration.Inf).size == gameMapping.size)
      assert(Await.result(dao.listResults, Duration.Inf).isEmpty)
      assert(Await.result(dao.listSeasons, Duration.Inf).size == 1)

    }

    "update games" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listTeams, Duration.Inf).isEmpty)
      assert(Await.result(dao.listGames, Duration.Inf).isEmpty)
      assert(Await.result(dao.listResults, Duration.Inf).isEmpty)
      assert(Await.result(dao.listSeasons, Duration.Inf).isEmpty)
      val s = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), Duration.Inf)
      private val teams = 1.to(350).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }).toList
      Await.result(Future.sequence(teams), Duration.Inf)
      val lst = Await.result(dao.listTeams, Duration.Inf)
      val rng = new Random(0L)
      val today = LocalDateTime.now

      private val gameMapping = 0.to(120).flatMap(n => {
        val split: (List[Team], List[Team]) = rng.shuffle(lst).splitAt(175)
        val pairs: List[(Team, Team)] = split._1.zip(split._2).take(20 + rng.nextInt(30))
        pairs.map(tup => MappedGame(Game(0L, s.id, tup._1.id, tup._2.id, today.plusDays(n).toLocalDate, today.plusDays(n), None, false, None, None, None, "", LocalDateTime.now(), "me")))
      }).toList

      svc.updateDb(List.empty[(Game, Option[Result])], gameMapping)

      assert(Await.result(dao.listTeams, Duration.Inf).size == 350)
      assert(Await.result(dao.listGames, Duration.Inf).size == gameMapping.size)
      assert(Await.result(dao.listResults, Duration.Inf).isEmpty)
      assert(Await.result(dao.listSeasons, Duration.Inf).size == 1)

      private val schedule = Await.result(dao.loadLatestSchedule(), Duration.Inf).get
      svc.updateDb(schedule.gameResults, gameMapping)

      assert(Await.result(dao.listTeams, Duration.Inf).size == 350)
      assert(Await.result(dao.listGames, Duration.Inf).size == gameMapping.size)
      assert(Await.result(dao.listResults, Duration.Inf).isEmpty)
      assert(Await.result(dao.listSeasons, Duration.Inf).size == 1)
    }

    "insert games & results" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listTeams, Duration.Inf).isEmpty)
      assert(Await.result(dao.listGames, Duration.Inf).isEmpty)
      assert(Await.result(dao.listResults, Duration.Inf).isEmpty)
      assert(Await.result(dao.listSeasons, Duration.Inf).isEmpty)
      val s = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), Duration.Inf)
      private val teams = 1.to(350).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }).toList
      Await.result(Future.sequence(teams), Duration.Inf)
      val lst = Await.result(dao.listTeams, Duration.Inf)
      val rng = new Random(0L)
      val today = LocalDateTime.now

      private val gameMapping = 0.to(120).flatMap(n => {
        val split: (List[Team], List[Team]) = rng.shuffle(lst).splitAt(175)
        val pairs: List[(Team, Team)] = split._1.zip(split._2).take(20 + rng.nextInt(30))
        pairs.map(tup => {
          val game = Game(0L, s.id, tup._1.id, tup._2.id, today.plusDays(n).toLocalDate, today.plusDays(n), None, false, None, None, None, "", LocalDateTime.now(), "me")
          if (rng.nextBoolean()) {
            MappedGame(game)
          }
          else {
            MappedGameAndResult(game, Result(0L, 0L, 50 + rng.nextInt(50), 50 + rng.nextInt(50), 2, LocalDateTime.now(), "me"))
          }
        })
      }).toList

      svc.updateDb(List.empty[(Game, Option[Result])], gameMapping)

      assert(Await.result(dao.listTeams, Duration.Inf).size == 350)
      assert(Await.result(dao.listGames, Duration.Inf).size == gameMapping.size)
      assert(Await.result(dao.listResults, Duration.Inf).nonEmpty)
      assert(Await.result(dao.listSeasons, Duration.Inf).size == 1)

      private val schedule = Await.result(dao.loadLatestSchedule(), Duration.Inf).get
      svc.updateDb(schedule.gameResults, gameMapping)

      assert(Await.result(dao.listTeams, Duration.Inf).size == 350)
      assert(Await.result(dao.listGames, Duration.Inf).size == gameMapping.size)
      assert(Await.result(dao.listResults, Duration.Inf).nonEmpty)
      assert(Await.result(dao.listSeasons, Duration.Inf).size == 1)
    }
//    "delete games & results" in new WithApplication(FakeApplication()) {
//      assert(Await.result(dao.listTeams, Duration.Inf).isEmpty)
//      assert(Await.result(dao.listGames, Duration.Inf).isEmpty)
//      assert(Await.result(dao.listResults, Duration.Inf).isEmpty)
//      assert(Await.result(dao.listSeasons, Duration.Inf).isEmpty)
//      val s = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), Duration.Inf)
//      private val teams = 1.to(350).map(n => {
//        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
//        dao.saveTeam(t)
//      }).toList
//      Await.result(Future.sequence(teams), Duration.Inf)
//      val lst = Await.result(dao.listTeams, Duration.Inf)
//      val rng = new Random(0L)
//      val today = LocalDateTime.now
//
//      private val gameMapping = 0.to(120).flatMap(n => {
//        val split: (List[Team], List[Team]) = rng.shuffle(lst).splitAt(175)
//        val pairs: List[(Team, Team)] = split._1.zip(split._2).take(20 + rng.nextInt(30))
//        pairs.map(tup => {
//          val game = Game(0L, s.id, tup._1.id, tup._2.id, today.plusDays(n).toLocalDate, today.plusDays(n), None, false, None, None, None, "", LocalDateTime.now(), "me")
//          if (rng.nextBoolean()) {
//            MappedGame(game)
//          }
//          else {
//            MappedGameAndResult(game, Result(0L, 0L, 50 + rng.nextInt(50), 50 + rng.nextInt(50), 2, LocalDateTime.now(), "me"))
//          }
//        })
//      }).toList
//
//      svc.updateDb(List.empty[(Game, Option[Result])], gameMapping)
//
//      assert(Await.result(dao.listTeams, Duration.Inf).size == 350)
//      assert(Await.result(dao.listGames, Duration.Inf).size == gameMapping.size)
//      assert(Await.result(dao.listResults, Duration.Inf).nonEmpty)
//      assert(Await.result(dao.listSeasons, Duration.Inf).size == 1)
//
//      private val schedule = Await.result(dao.loadLatestSchedule(), Duration.Inf).get
//      svc.updateDb(schedule.gameResults, gameMapping.drop(1000))
//
//      assert(Await.result(dao.listTeams, Duration.Inf).size == 350)
//      assert(Await.result(dao.listGames, Duration.Inf).size == gameMapping.size-1000)
//      assert(Await.result(dao.listResults, Duration.Inf).nonEmpty)
//      assert(Await.result(dao.listSeasons, Duration.Inf).size == 1)
//    }


//    "modify games & results" in new WithApplication(FakeApplication()) {
//      assert(Await.result(dao.listTeams, Duration.Inf).isEmpty)
//      assert(Await.result(dao.listGames, Duration.Inf).isEmpty)
//      assert(Await.result(dao.listResults, Duration.Inf).isEmpty)
//      assert(Await.result(dao.listSeasons, Duration.Inf).isEmpty)
//      val s = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), Duration.Inf)
//      private val teams = 1.to(350).map(n => {
//        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
//        dao.saveTeam(t)
//      }).toList
//      Await.result(Future.sequence(teams), Duration.Inf)
//      val lst = Await.result(dao.listTeams, Duration.Inf)
//      val rng = new Random(0L)
//      val today = LocalDateTime.now
//
//      private val gameMapping = 0.to(120).flatMap(n => {
//        val split: (List[Team], List[Team]) = rng.shuffle(lst).splitAt(175)
//        val pairs: List[(Team, Team)] = split._1.zip(split._2).take(20 + rng.nextInt(30))
//        pairs.map(tup => {
//          val game = Game(0L, s.id, tup._1.id, tup._2.id, today.plusDays(n).toLocalDate, today.plusDays(n), None, false, None, None, None, "", LocalDateTime.now(), "me")
//          if (rng.nextBoolean()) {
//            MappedGame(game)
//          }
//          else {
//            MappedGameAndResult(game, Result(0L, 0L, 50 + rng.nextInt(50), 50 + rng.nextInt(50), 2, LocalDateTime.now(), "me"))
//          }
//        })
//      }).toList
//
//      svc.updateDb(List.empty[(Game, Option[Result])], gameMapping)
//
//      assert(Await.result(dao.listTeams, Duration.Inf).size == 350)
//      assert(Await.result(dao.listGames, Duration.Inf).size == gameMapping.size)
//      assert(Await.result(dao.listResults, Duration.Inf).nonEmpty)
//      assert(Await.result(dao.listSeasons, Duration.Inf).size == 1)
//
//      private val schedule = Await.result(dao.loadLatestSchedule(), Duration.Inf).get
//      private val gameMapping2 = gameMapping.map {
//        case MappedGameAndResult(g, _) => MappedGame(g)
//        case x:MappedGame => x
//      }
//
//      svc.updateDb(schedule.gameResults, gameMapping2)
//
//      assert(Await.result(dao.listTeams, Duration.Inf).size == 350)
//      assert(Await.result(dao.listGames, Duration.Inf).size == gameMapping.size)
//      assert(Await.result(dao.listResults, Duration.Inf).isEmpty)
//      assert(Await.result(dao.listSeasons, Duration.Inf).size == 1)
//    }
  }
}
