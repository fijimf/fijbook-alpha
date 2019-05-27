package com.fijimf.deepfij.models

import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await



//TODO there was a bona-fide bug here which would have been caught by tests
class RepoConferenceMapSpec extends PlaySpec with GuiceOneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin  with ScalaFutures {
  val dao = Injector.inject[ScheduleDAO]

  "ConferenceMaps " should {
    "be empty initially" in new WithApplication() {
      assert(Await.result(dao.listConferenceMaps, testDbTimeout).isEmpty)
    }

    "allow a conference mapping to be saved" in new WithApplication() {
      assert(Await.result(dao.listConferenceMaps, testDbTimeout).isEmpty)
      val seas = Await.result(dao.saveSeason(Season(0L, 2019)), testDbTimeout)
      val conf = saveQuickConf("big-sky", "Big Sky")
      val team = saveQuickTeam("idaho", "Idaho", "Vandals")
      private val cm: ConferenceMap = Await.result(dao.saveConferenceMap(ConferenceMap(0L, seas.id, conf.id, team.id, LocalDateTime.now(), "")), testDbTimeout)
      assert(cm.id > 0L)
      assert(cm.seasonId === seas.id)
      assert(cm.conferenceId === conf.id)
      assert(cm.teamId === team.id)
    }


    "allow a list of conference mapping to be saved" in new WithApplication() {
      assert(Await.result(dao.listConferenceMaps, testDbTimeout).isEmpty)
      val seas = Await.result(dao.saveSeason(Season(0L, 2019)), testDbTimeout)
      val conf1 = saveQuickConf("big-sky", "Big Sky")
      val conf2 = saveQuickConf("sun-belt", "Sun Belt")
      val team1 = saveQuickTeam("idaho", "Idaho", "Vandals")
      val team2 = saveQuickTeam("montana-st", "Montana State", "Bobcats")
      val team3 = saveQuickTeam("troy", "Troy", "Trojans")
      val team4 = saveQuickTeam("georgia-st", "Georgia State", "Bears?")
      private val cms: List[ConferenceMap] = Await.result(dao.saveConferenceMaps(
        List(
          ConferenceMap(0L, seas.id, conf1.id, team1.id, LocalDateTime.now(), ""),
          ConferenceMap(0L, seas.id, conf1.id, team2.id, LocalDateTime.now(), ""),
          ConferenceMap(0L, seas.id, conf2.id, team3.id, LocalDateTime.now(), ""),
          ConferenceMap(0L, seas.id, conf2.id, team4.id, LocalDateTime.now(), "")
        )
      ), testDbTimeout
      )
      assert(cms.forall(_.id > 0L))
      assert(cms.size === 4)
    }
    "allow a conference mapping to be updated" in new WithApplication() {
      assert(Await.result(dao.listConferenceMaps, testDbTimeout).isEmpty)
      val seas = Await.result(dao.saveSeason(Season(0L, 2019)), testDbTimeout)
      val conf1 = saveQuickConf("big-sky", "Big Sky")
      val conf2 = saveQuickConf("sun-belt", "Sun Belt")
      val team1 = saveQuickTeam("idaho", "Idaho", "Vandals")
      val team2 = saveQuickTeam("montana-st", "Montana State", "Bobcats")
      val team3 = saveQuickTeam("troy", "Troy", "Trojans")
      val team4 = saveQuickTeam("georgia-st", "Georgia State", "Bears?")
      private val cms: List[ConferenceMap] = Await.result(dao.saveConferenceMaps(
        List(
          ConferenceMap(0L, seas.id, conf2.id, team1.id, LocalDateTime.now(), ""),
          ConferenceMap(0L, seas.id, conf1.id, team2.id, LocalDateTime.now(), ""),
          ConferenceMap(0L, seas.id, conf2.id, team3.id, LocalDateTime.now(), ""),
          ConferenceMap(0L, seas.id, conf2.id, team4.id, LocalDateTime.now(), "")
        )
      ), testDbTimeout
      )
      assert(cms.forall(_.id > 0L))
      assert(cms.size === 4)
      cms.find(_.teamId === team1.id) match {
        case Some(idaho) =>
          assert(idaho.conferenceId === conf2.id) //Idaho is misaligned into Sun Belt
          val jdaho = idaho.copy(conferenceId = conf1.id)
          val kdaho = Await.result(dao.saveConferenceMap(jdaho), testDbTimeout)
          assert(jdaho.id === kdaho.id)
          assert(idaho.id === kdaho.id)
          assert(kdaho.conferenceId === conf1.id)
        case None =>
          fail("Should never happen -- already tested for")
      }
    }

  }


  def saveQuickConf(key: String, name: String): Conference = {
    Await.result(dao.saveConference(Conference(0L, key, name, "Unknown", None, None, None, None, None, LocalDateTime.now(), "Test")), testDbTimeout)
  }

  def saveQuickTeam(key: String, name: String, nickname: String): Team = {
    Await.result(dao.saveTeam(Team(0L, key, name, name, nickname, "", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")), testDbTimeout)
  }



}
