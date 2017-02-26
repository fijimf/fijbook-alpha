package com.fijimf.deepfij.models

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.test.{FakeApplication, WithApplication}
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class RepoUserProfileSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val repo = Injector.inject[ScheduleRepository]
  val dao = Injector.inject[ScheduleDAO]

  override def beforeEach() = {
    Await.result(repo.createSchema(), Duration.Inf)
  }

  override def afterEach() = {
    Await.result(repo.dropSchema(), Duration.Inf)
  }

  "UserProfiles " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listUserProfiles, Duration.Inf).isEmpty)
    }

    "can save user profile data" in new WithApplication(FakeApplication()) {
      val userId = "user-123"
      val data = Map("k1" -> "value", "k2" -> "value", "k3" -> "other value")
      assert(Await.result(dao.saveUserProfile(userId, data), Duration.Inf).size == 3)
      private val userProfileList = Await.result(dao.listUserProfiles, Duration.Inf)
      assert(userProfileList.size == 3)
      assert(userProfileList.map(u => u.key -> u.value).toMap == data)
    }

    "can update user profile data" in new WithApplication(FakeApplication()) {
      val userId = "user-123"
      val data = Map("k1" -> "value", "k2" -> "value", "k3" -> "other value")
      assert(Await.result(dao.saveUserProfile(userId, data), Duration.Inf).size == 3)
      private val userProfileList = Await.result(dao.listUserProfiles, Duration.Inf)
      assert(userProfileList.size == 3)
      assert(userProfileList.map(u => u.key -> u.value).toMap == data)
      val data2 = Map("k1" -> "value", "k3" -> "changed", "m2" -> "new one")
      assert(Await.result(dao.saveUserProfile(userId, data2), Duration.Inf).size == 3)
      private val userProfileList2 = Await.result(dao.listUserProfiles, Duration.Inf)
      assert(userProfileList2.size == 3)
      assert(userProfileList2.map(u => u.key -> u.value).toMap == data2)
    }

    "can load profile data for a user" in new WithApplication(FakeApplication()) {
      val userId1 = "user-123"
      val userId2 = "user-456"
      val data1 = Map("k1" -> "value", "k2" -> "value", "k3" -> "other value")
      val data2 = Map("m1" -> "value", "m2" -> "value", "m3" -> "other value")
      assert(Await.result(dao.saveUserProfile(userId1, data1), Duration.Inf).size == 3)
      assert(Await.result(dao.saveUserProfile(userId2, data2), Duration.Inf).size == 3)
      assert(Await.result(dao.listUserProfiles, Duration.Inf).size == 6)

      assert(Await.result(dao.findUserProfile("XXXXX"), Duration.Inf).isEmpty)
      assert(Await.result(dao.findUserProfile("user-456"), Duration.Inf) == data2)
    }

    "can delete profile data for a user" in new WithApplication(FakeApplication()) {
      val userId1 = "user-123"
      val userId2 = "user-456"
      val data1 = Map("k1" -> "value", "k2" -> "value", "k3" -> "other value")
      val data2 = Map("m1" -> "value", "m2" -> "value", "m3" -> "other value")
      assert(Await.result(dao.saveUserProfile(userId1, data1), Duration.Inf).size == 3)
      assert(Await.result(dao.saveUserProfile(userId2, data2), Duration.Inf).size == 3)
      assert(Await.result(dao.listUserProfiles, Duration.Inf).size == 6)

      assert(Await.result(dao.deleteUserProfile("user-123"), Duration.Inf) == 3)
      assert(Await.result(dao.listUserProfiles, Duration.Inf).size == 3)
      assert(Await.result(dao.deleteUserProfile("user-xxx"), Duration.Inf) == 0)
      assert(Await.result(dao.listUserProfiles, Duration.Inf).size == 3)
      assert(Await.result(dao.deleteUserProfile("user-456"), Duration.Inf) == 3)
      assert(Await.result(dao.listUserProfiles, Duration.Inf).size == 0)
    }
  }
}