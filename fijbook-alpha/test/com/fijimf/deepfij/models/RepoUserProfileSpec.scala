package com.fijimf.deepfij.models

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.test.WithApplication
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration._

class RepoUserProfileSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin  with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]


  "UserProfiles " should {
    "be empty initially" in new WithApplication() {
      assert(Await.result(dao.listUserProfiles, testDbTimeout).isEmpty)
    }

    "can save user profile data" in new WithApplication() {
      val userId = "user-123"
      val data = Map("k1" -> "value", "k2" -> "value", "k3" -> "other value")
      assert(Await.result(dao.saveUserProfile(userId, data), testDbTimeout).size === 3)
      private val userProfileList = Await.result(dao.listUserProfiles, testDbTimeout)
      assert(userProfileList.size === 3)
      assert(userProfileList.map(u => u.key -> u.value).toMap === data)
    }

    "can update user profile data" in new WithApplication() {
      val userId = "user-123"
      val data = Map("k1" -> "value", "k2" -> "value", "k3" -> "other value")
      assert(Await.result(dao.saveUserProfile(userId, data), testDbTimeout).size === 3)
      private val userProfileList = Await.result(dao.listUserProfiles, testDbTimeout)
      assert(userProfileList.size === 3)
      assert(userProfileList.map(u => u.key -> u.value).toMap === data)
      val data2 = Map("k1" -> "value", "k3" -> "changed", "m2" -> "new one")
      assert(Await.result(dao.saveUserProfile(userId, data2), testDbTimeout).size === 3)
      private val userProfileList2 = Await.result(dao.listUserProfiles, testDbTimeout)
      assert(userProfileList2.size === 3)
      assert(userProfileList2.map(u => u.key -> u.value).toMap === data2)
    }

    "can load profile data for a user" in new WithApplication() {
      val userId1 = "user-123"
      val userId2 = "user-456"
      val data1 = Map("k1" -> "value", "k2" -> "value", "k3" -> "other value")
      val data2 = Map("m1" -> "value", "m2" -> "value", "m3" -> "other value")
      assert(Await.result(dao.saveUserProfile(userId1, data1), testDbTimeout).size === 3)
      assert(Await.result(dao.saveUserProfile(userId2, data2), testDbTimeout).size === 3)
      assert(Await.result(dao.listUserProfiles, testDbTimeout).size === 6)

      assert(Await.result(dao.findUserProfile("XXXXX"), testDbTimeout).isEmpty)
      assert(Await.result(dao.findUserProfile("user-456"), testDbTimeout) === data2)
    }

    "can delete profile data for a user" in new WithApplication() {
      val userId1 = "user-123"
      val userId2 = "user-456"
      val data1 = Map("k1" -> "value", "k2" -> "value", "k3" -> "other value")
      val data2 = Map("m1" -> "value", "m2" -> "value", "m3" -> "other value")
      assert(Await.result(dao.saveUserProfile(userId1, data1), testDbTimeout).size === 3)
      assert(Await.result(dao.saveUserProfile(userId2, data2), testDbTimeout).size === 3)
      assert(Await.result(dao.listUserProfiles, testDbTimeout).size === 6)

      assert(Await.result(dao.deleteUserProfile("user-123"), testDbTimeout) === 3)
      assert(Await.result(dao.listUserProfiles, testDbTimeout).size === 3)
      assert(Await.result(dao.deleteUserProfile("user-xxx"), testDbTimeout) === 0)
      assert(Await.result(dao.listUserProfiles, testDbTimeout).size === 3)
      assert(Await.result(dao.deleteUserProfile("user-456"), testDbTimeout) === 3)
      assert(Await.result(dao.listUserProfiles, testDbTimeout).size === 0)
    }
  }
}