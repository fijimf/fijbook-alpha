package com.fijimf.deepfij.models

import java.time.LocalDateTime

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class TeamDAOImplSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val repo = Injector.inject[ScheduleRepository]
  val teamDao = Injector.inject[ScheduleDAOImpl]

  override def beforeEach() = {
    Await.result(repo.createSchema(), Duration.Inf)
  }

  override def afterEach() = {
    Await.result(repo.dropSchema(), Duration.Inf)
  }

  "TeamDao " should {

    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(teamDao.list, Duration.Inf).isEmpty)
    }
    "allow teams to be created" in new WithApplication(FakeApplication()) {
      private val team = Team(0L, "georgetown", "Georgetown", "Georgetown", "Hoyas", None, None, None, None, None, None, None, false, LocalDateTime.now(), "Jim")
      private val rowsAffected: Int = Await.result(teamDao.save(team), Duration.Inf)
      assert(rowsAffected == 1)
      private val teamList: List[Team] = Await.result(teamDao.list, Duration.Inf)
      assert(teamList.size == 1)
      assert(teamList(0).key === "georgetown")
      assert(teamList(0).name === "Georgetown")
      assert(teamList(0).nickname === "Hoyas")
      val id = teamList(0).id
      assert(id > 0)
      private val idResult: Option[Team] = Await.result(teamDao.find(id), Duration.Inf)
      assert(idResult.isDefined)
      assert(idResult.get.name === "Georgetown")
      private val keyResult: Option[Team] = Await.result(teamDao.find("georgetown"), Duration.Inf)
      assert(keyResult.isDefined)
      assert(keyResult.get.name === "Georgetown")
    }

    "allow teams to be updated" in new WithApplication(FakeApplication()) {
      private val team1 = Team(0L, "georgetown", "Georgetown", "Georgetown", "Xoyas", None, None, None, None, None, None, None, false, LocalDateTime.now(), "Jim")
      private val rowsAffected1: Int = Await.result(teamDao.save(team1), Duration.Inf)
      private val team2 = Team(0L, "georgetown", "Georgetown", "Georgetown", "Hoyas", None, None, None, None, None, None, None, false, LocalDateTime.now(), "Jim")
      private val rowsAffected2: Int = Await.result(teamDao.save(team2), Duration.Inf)

      assert(rowsAffected2 == 1)
      private val teamList: List[Team] = Await.result(teamDao.list, Duration.Inf)
      assert(teamList.size == 1)
      assert(teamList(0).key === "georgetown")
      assert(teamList(0).name === "Georgetown")
      assert(teamList(0).nickname === "Hoyas")
      val id = teamList(0).id
      assert(id > 0)
    }

    "locked team cannot be updated" in new WithApplication(FakeApplication()) {
      private val team1 = Team(0L, "georgetown", "Georgetown", "Georgetown", "Xoyas", None, None, None, None, None, None, None, true, LocalDateTime.now(), "Jim")
      private val rowsAffected1: Int = Await.result(teamDao.save(team1), Duration.Inf)
      private val team2 = Team(0L, "georgetown", "Georgetown", "Georgetown", "Hoyas", None, None, None, None, None, None, None, false, LocalDateTime.now(), "Jim")
      private val rowsAffected2: Int = Await.result(teamDao.save(team2), Duration.Inf)

      assert(rowsAffected2 == 0)
      private val teamList: List[Team] = Await.result(teamDao.list, Duration.Inf)
      assert(teamList.size == 1)
      assert(teamList(0).key === "georgetown")
      assert(teamList(0).name === "Georgetown")
      assert(teamList(0).nickname === "Xoyas")
      val id = teamList(0).id
      assert(id > 0)
    }
 //TODO lock; unlock;
    }
}
