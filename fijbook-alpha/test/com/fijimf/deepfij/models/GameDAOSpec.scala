package com.fijimf.deepfij.models

import java.sql.SQLException
import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await

class GameDAOSpec extends PlaySpec with GuiceOneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {
  val dao: ScheduleDAO = Injector.inject[ScheduleDAO]


  "Games " should {
    "be empty initially" in new WithApplication() {
      assert(Await.result(dao.listGames, testDbTimeout).isEmpty)
    }

    "be able to save one new game" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      val g: Game = Await.result(dao.updateGame(shorthandGame(season, teams(1), teams(2), LocalDate.now())), testDbTimeout)
      assert(g.id > 0)
      assert(g.seasonId === season.id)
      assert(g.homeTeamId === teams(1).id)
      assert(g.awayTeamId === teams(2).id)
      assert(Await.result(dao.listGames, testDbTimeout).size === 1)
    }

    "be able to change and update one game" in new WithApplication() {
      val teams: List[Team] = load4Teams

      val season: Season = loadSeason

      val g0: Game = Await.result(dao.updateGame(shorthandGame(season, teams(1), teams(2), LocalDate.now())), testDbTimeout)
      val g1: Game = Await.result(dao.updateGame(g0.copy(homeTeamId = teams(3).id)), testDbTimeout)
      assert(g1.id === g0.id)
      assert(g1.seasonId === season.id)
      assert(g1.homeTeamId === teams(3).id)
      assert(g1.awayTeamId === teams(2).id)
      assert(Await.result(dao.listGames, testDbTimeout).size === 1)
    }

    "fail to save one new game if home is the same as away" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      try {
        val g: Game = Await.result(dao.updateGame(shorthandGame(season,teams(2),teams(2),LocalDate.now())), testDbTimeout)
        fail()
      } catch {
        case _:IllegalArgumentException=> //OK
      }

    }

    "fail to save one new game if teams violate referential integrity" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      try {
        val g: Game = Await.result(dao.updateGame(shorthandGame(season.id, -99L, teams(2).id, LocalDate.now())), testDbTimeout)
        fail()
      } catch {
        case _: SQLException => //OK
      }
      try {
        val g: Game = Await.result(dao.updateGame(shorthandGame(season.id, teams(1).id, -99L, LocalDate.now())), testDbTimeout)
        fail()
      } catch {
        case _: SQLException => //OK
      }
    }

    "fail to save one new game if season violates referential integrity" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      try {
        val g: Game = Await.result(dao.updateGame(shorthandGame(-990L,teams(2).id,teams(3).id,LocalDate.now())), testDbTimeout)
        fail()
      } catch {
        case _:SQLException=> //OK
      }

    }

    "fail to save games with duplicate signature" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      try {
        Await.result(dao.updateGame(shorthandGame(season.id,teams(2).id,teams(3).id,LocalDate.now())), testDbTimeout)
        Await.result(dao.updateGame(shorthandGame(season.id,teams(2).id,teams(3).id,LocalDate.now())), testDbTimeout)
        fail()
      } catch {
        case _:SQLException=> //OK
      }

    }

    "be able to be saved in bulk" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      private val d: LocalDate = LocalDate.now()
      val gs: List[Game] = Await.result(dao.updateGames(List(
        shorthandGame(season, teams(0), teams(2), d),
        shorthandGame(season, teams(1), teams(3), d),
        shorthandGame(season, teams(1), teams(2), d.plusDays(2)),
        shorthandGame(season, teams(3), teams(0), d.plusDays(2)),
        shorthandGame(season, teams(1), teams(3), d.plusDays(4)),
        shorthandGame(season, teams(1), teams(0), d.plusDays(4)),
        shorthandGame(season, teams(3), teams(2), d.plusDays(5))
      )), testDbTimeout)

      assert(Await.result(dao.listGames, testDbTimeout).size === 7)
      gs.foreach(g=>assert(g.id>0L))
    }

    "be able to be updated in bulk" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      private val d: LocalDate = LocalDate.now()
      val gs: List[Game] = Await.result(dao.updateGames(List(
        shorthandGame(season, teams(0), teams(2), d),
        shorthandGame(season, teams(1), teams(3), d),
        shorthandGame(season, teams(1), teams(2), d.plusDays(2)),
        shorthandGame(season, teams(3), teams(0), d.plusDays(2)),
        shorthandGame(season, teams(1), teams(3), d.plusDays(4)),
        shorthandGame(season, teams(1), teams(0), d.plusDays(4)),
        shorthandGame(season, teams(3), teams(2), d.plusDays(5))
      )), testDbTimeout)

      assert(Await.result(dao.listGames, testDbTimeout).size === 7)
      Await.result(dao.updateGames(gs.map(g=>g.copy(homeTeamId = g.awayTeamId,awayTeamId = g.homeTeamId))),testDbTimeout)
      private val games: List[Game] = Await.result(dao.listGames, testDbTimeout)
      assert(games.size === 7)
      gs.zip(games).foreach{case (x,y)=>
        assert(x.homeTeamId===y.awayTeamId)
        assert(x.awayTeamId===y.homeTeamId)
        assert(x.date===y.date)
        assert(x.seasonId===y.seasonId)
      }
    }

    "be able to be saved in bulk as a SINGLE TRANSACTION (ref int failure)" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      private val d: LocalDate = LocalDate.now()
      try {
        val gs: List[Game] = Await.result(dao.updateGames(List(
          shorthandGame(season, teams(0), teams(2), d),
          shorthandGame(season, teams(1), teams(3), d),
          shorthandGame(season, teams(1), teams(2), d.plusDays(2)),
          shorthandGame(season, teams(3), teams(0), d.plusDays(2)),
          shorthandGame(season.id, -99L, teams(3).id, d.plusDays(4)),
          shorthandGame(season, teams(1), teams(0), d.plusDays(4)),
          shorthandGame(season, teams(3), teams(2), d.plusDays(5))
        )), testDbTimeout)
        fail()
      }catch {
        case _:SQLException=> //OK
      }
      assert(Await.result(dao.listGames, testDbTimeout).size === 0)

    }
    "be able to be saved in bulk as a SINGLE TRANSACTION (same team failure)" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      private val d: LocalDate = LocalDate.now()
      try {
        val gs: List[Game] = Await.result(dao.updateGames(List(
          shorthandGame(season, teams(0), teams(2), d),
          shorthandGame(season, teams(1), teams(3), d),
          shorthandGame(season, teams(1), teams(2), d.plusDays(2)),
          shorthandGame(season, teams(3), teams(0), d.plusDays(2)),
          shorthandGame(season, teams(1), teams(1), d.plusDays(4)),
          shorthandGame(season, teams(1), teams(0), d.plusDays(4)),
          shorthandGame(season, teams(3), teams(2), d.plusDays(5))
        )), testDbTimeout)
        fail()
      }catch {
        case _:IllegalArgumentException=> //OK
      }
      assert(Await.result(dao.listGames, testDbTimeout).size === 0)

    }


    "be able to save one new game WITH NO result" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      val g: (Game, Option[Result]) = Await.result(dao.updateGameWithResult(shorthandGame(season, teams(1), teams(2), LocalDate.now()),Option.empty[Result]), testDbTimeout)

      assert(g._1.id > 0)
      assert(g._1.seasonId === season.id)
      assert(g._1.homeTeamId === teams(1).id)
      assert(g._1.awayTeamId === teams(2).id)
      assert(Await.result(dao.listGames, testDbTimeout).size === 1)
      assert(Await.result(dao.listResults, testDbTimeout).size === 0)
    }

    "be able to save one new game WITH result" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      val g: (Game, Option[Result]) = Await.result(dao.updateGameWithResult(shorthandGame(season, teams(1), teams(2), LocalDate.now()),Some(Result(0L,0L,100,98,2,LocalDateTime.now(),"Test"))), testDbTimeout)

      assert(g._1.id > 0)
      assert(g._1.seasonId === season.id)
      assert(g._1.homeTeamId === teams(1).id)
      assert(g._1.awayTeamId === teams(2).id)
      assert(g._2.isDefined)
      assert(g._2.map(_.gameId)===Some(g._1.id))
      assert(g._2.map(_.homeScore)===Some(100))
      assert(g._2.map(_.awayScore)===Some(98))
      assert(Await.result(dao.listGames, testDbTimeout).size === 1)
      assert(Await.result(dao.listResults, testDbTimeout).size === 1)
    }

    "be able to update game WITH result" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      val g: (Game, Option[Result]) =
        Await.result(
          dao.updateGameWithResult(shorthandGame(season, teams(1), teams(2), LocalDate.now()),Some(Result(0L,0L,100,98,2,LocalDateTime.now(),"Test"))
          ), testDbTimeout
        )

      assert(g._1.id > 0)
      assert(g._1.seasonId === season.id)
      assert(g._1.homeTeamId === teams(1).id)
      assert(g._1.awayTeamId === teams(2).id)
      assert(g._2.isDefined)
      assert(g._2.map(_.gameId)===Some(g._1.id))
      assert(g._2.map(_.homeScore)===Some(100))
      assert(g._2.map(_.awayScore)===Some(98))
      assert(Await.result(dao.listGames, testDbTimeout).size === 1)
      assert(Await.result(dao.listResults, testDbTimeout).size === 1)

      val h: (Game,Option[Result]) = Await.result(
        dao.updateGameWithResult(g._1.copy(homeTeamId = teams(3).id),g._2
        ), testDbTimeout
      )

      assert(g._1.id === h._1.id)
      assert(g._1.seasonId === h._1.seasonId )
      assert(h._1.homeTeamId === teams(3).id)
      assert(h._1.awayTeamId === teams(2).id)
      assert(h._2.isDefined)
      assert(h._2.map(_.gameId)===Some(h._1.id))
      assert(h._2.map(_.homeScore)===Some(100))
      assert(h._2.map(_.awayScore)===Some(98))
      assert(Await.result(dao.listGames, testDbTimeout).size === 1)
      assert(Await.result(dao.listResults, testDbTimeout).size === 1)

    }


    "be able to update game and REMOVE result" in new WithApplication() {
      val teams: List[Team] = load4Teams
      val season: Season = loadSeason
      val g: (Game, Option[Result]) =
        Await.result(
          dao.updateGameWithResult(shorthandGame(season, teams(1), teams(2), LocalDate.now()),Some(Result(0L,0L,100,98,2,LocalDateTime.now(),"Test"))
          ), testDbTimeout
        )

      assert(g._1.id > 0)
      assert(g._1.seasonId === season.id)
      assert(g._1.homeTeamId === teams(1).id)
      assert(g._1.awayTeamId === teams(2).id)
      assert(g._2.isDefined)
      assert(g._2.map(_.gameId)===Some(g._1.id))
      assert(g._2.map(_.homeScore)===Some(100))
      assert(g._2.map(_.awayScore)===Some(98))
      assert(Await.result(dao.listGames, testDbTimeout).size === 1)
      assert(Await.result(dao.listResults, testDbTimeout).size === 1)

      val h: (Game,Option[Result]) = Await.result(
        dao.updateGameWithResult(g._1.copy(homeTeamId = teams(3).id),Option.empty[Result]
        ), testDbTimeout
      )

      assert(g._1.id === h._1.id)
      assert(g._1.seasonId === h._1.seasonId )
      assert(h._1.homeTeamId === teams(3).id)
      assert(h._1.awayTeamId === teams(2).id)
      assert(h._2.isEmpty)
      assert(Await.result(dao.listGames, testDbTimeout).size === 1)
      assert(Await.result(dao.listResults, testDbTimeout).size === 0)

    }

  }

  private def shorthandGame(season: Season, homeTeam: Team, awayTeam: Team, date: LocalDate): Game = {
    Game(0L, season.id, homeTeam.id, awayTeam.id, date, date.atTime(12, 0), None, false, None, None, None, LocalDate.now().toString, LocalDateTime.now(), "TEST")
  }

  private def shorthandGame(seasonId: Long, homeTeamId: Long, awayTeamId: Long, date: LocalDate): Game = {
    Game(0L, seasonId, homeTeamId, awayTeamId, date, date.atTime(12, 0), None, false, None, None, None, LocalDate.now().toString, LocalDateTime.now(), "TEST")
  }

  private def loadSeason = {
    Await.result(dao.saveSeason(Season(0L, LocalDate.now.getYear)), testDbTimeout)
  }

  private def load4Teams = {
    Await.result(dao.saveTeams(List(
      Team(0L, "A", "A", "A", "As", "", None, None, None, None, None, None, None, LocalDateTime.now(), "TEST"),
      Team(0L, "B", "B", "B", "Bs", "", None, None, None, None, None, None, None, LocalDateTime.now(), "TEST"),
      Team(0L, "C", "C", "C", "Cs", "", None, None, None, None, None, None, None, LocalDateTime.now(), "TEST"),
      Team(0L, "D", "D", "D", "Ds", "", None, None, None, None, None, None, None, LocalDateTime.now(), "TEST")
    )), testDbTimeout)
  }
}

