package com.fijimf.deepfij.models.nstats

import java.io.InputStream
import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.{ScheduleSerializer, ScheduleSerializerSpec}
import com.fijimf.deepfij.models.{Game, RebuildDatabaseMixin, Result}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.libs.json.Json
import testhelpers.Injector

import scala.concurrent.Await
import scala.io.Source

class NSTatsTesterSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin {

  type SB = com.fijimf.deepfij.models.nstats.Scoreboard

  val dao: ScheduleDAO = Injector.inject[ScheduleDAO]

  import ScheduleSerializer._

  import scala.concurrent.ExecutionContext.Implicits.global

  val isS3Json: InputStream = classOf[ScheduleSerializerSpec].getResourceAsStream("/test-data/s3Sched.json")
  val s3Sched: Array[Byte] = Source.fromInputStream(isS3Json).mkString.getBytes


  "Regression State " should {
    "maintain its invariants when initialized" in {
      val r = Regression.RegState()
      assert(r.keyMap.isEmpty)
      assert(r.A.length == 0)
      assert(r.b.length == 0)
    }

    "maintain its invariants when a team is added" in {
      val r = Regression.RegState().addTeam(33L).addTeam(123L)
      assert(r.keyMap.size == 2)
      assert(r.keyMap(33L) == 0)
      assert(r.keyMap(123L) == 1)
      assert(r.A.length == 0)
      assert(r.b.length == 0)

      val s = r.addTeam(33L).addTeam(123L)
      assert(s.keyMap.size == 2)
      assert(s.keyMap(33L) == 0)
      assert(s.keyMap(123L) == 1)
      assert(s.A.length == 0)
      assert(s.b.length == 0)
    }

    "create a game row" in {

      val r = Regression.RegState().addTeam(33L).addTeam(123L).addTeam(99L)
      val row = r.gameRowDiff(33L, 123L)

      assert(row.length == 3)
      assert(row(0) == 1.0)
      assert(row(1) == -1.0)
      assert(row(2) == 0.0)
    }

    "maintain invariants when a game is added" in {
      val r = Regression.RegState().addGame(createTestGame(33L, 42L, LocalDate.now()), Result(0L, 0L, 100, 75, 2, LocalDateTime.now(), "Test"))
    }
  }

  private def createTestGame(homeId: Long, awayId: Long, date: LocalDate) = {
    Game(-1L, -1L, homeId, awayId, date, LocalDateTime.now(), None, false, None, None, None, "Test", LocalDateTime.now(), "Test")
  }

  "The Analysis object " should {

    "load a schedule and loop over it to calculate stats" in {
      Json.parse(s3Sched).asOpt[MappedUniverse] match {
        case Some(uni) =>
          Await.result(saveToDb(uni, dao, repo), testDbTimeout)
          assert(Await.result(dao.listTeams.map(_.size), testDbTimeout) === 351)
          assert(Await.result(dao.listConferences.map(_.size), testDbTimeout) === 32)
          assert(Await.result(dao.listAliases.map(_.size), testDbTimeout) === 161)
          assert(Await.result(dao.listSeasons.map(_.size), testDbTimeout) === 5)
          val ss = Await.result(dao.loadSchedules(), testDbTimeout)
          ss.foreach(s => {
            println(s"${s.season.year} ${s.games.size}")
          })

          Analysis.analyzeSchedule(ss(0), Counters.games)
          Analysis.analyzeSchedule(ss(0), Counters.wins)
          Analysis.analyzeSchedule(ss(0), Counters.losses)
          Analysis.analyzeSchedule(ss(0), Counters.otGames)
          Analysis.analyzeSchedule(ss(0), Counters.homeLosses)
          Analysis.analyzeSchedule(ss(0), Counters.awayWins)
          Analysis.analyzeSchedule(ss(0), Counters.otLosses)
          Analysis.analyzeSchedule(ss(0), Appenders.meanMargin)
          Analysis.analyzeSchedule(ss(0), Appenders.varianceMargin)
          Analysis.analyzeSchedule(ss(0), Counters.winStreak)
          Analysis.analyzeSchedule(ss(0), HigherOrderCounters.oppWins)
          Analysis.analyzeSchedule(ss(0), Regression.ols)

        case None => fail()
      }
    }
  }


}