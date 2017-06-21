package com.fijimf.deepfij.models

import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await


class RepoConferenceSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin  with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]

  "Conferences " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listConferences, testDbTimeout).isEmpty)
    }
  }


  /////



  "return the new ID when inserted" in new WithApplication(FakeApplication()) {
    assertConferencesIsEmpty()
    val t = mkQuickConf(0L, "big-east", "Big East")
    val s = Await.result(dao.saveConference(t), testDbTimeout)
    assertConferencesSize(1)
    compareConferences(s, t)
  }

  "return the old ID when updated" in new WithApplication(FakeApplication()) {
    assertConferencesIsEmpty()
    val t = mkQuickConf(0L, "big-east", "BigEast")
    val s = Await.result(dao.saveConference(t), testDbTimeout)
    assertConferencesSize(1)
    compareConferences(s, t)
    val u = t.copy(id = s.id, name = "Big East")
    val v = Await.result(dao.saveConference(u), testDbTimeout)
    assertConferencesSize(1)
    compareConferences(u, v)
    val w = Await.result(dao.listConferences, testDbTimeout).head
    assert(w.id == u.id)
    assert(w.id == v.id)
    assert(w.name == "Big East")
  }

  "not be inserted with the same key as an existing conference" in new WithApplication(FakeApplication()) {
    assertConferencesIsEmpty()
    val t1 = mkQuickConf(0L, "big-ten", "Big Ten")
    Await.result(dao.saveConference(t1), testDbTimeout)
    val t2 = mkQuickConf(0L, "big-ten", "Big 10")
    try {
      Await.result(dao.saveConference(t2), testDbTimeout)
      fail
    } catch {
      case _: Throwable => //OK
    }
  }

  "not be updated with the same key as an existing conference" in new WithApplication(FakeApplication()) {
    assertConferencesIsEmpty()
    val t1 = mkQuickConf(0L, "big-t", "Big Ten")
    Await.result(dao.saveConference(t1), testDbTimeout)
    val t2= mkQuickConf(0L, "big-twelve", "Big 12")
    val id2 = Await.result(dao.saveConference(t2), testDbTimeout)

    val t3 = t2.copy(id = id2.id, key = "big-t")
    try {
      Await.result(dao.saveConference(t3), testDbTimeout)
      fail
    } catch {
      case _: Throwable => //OK
    }
  }

  "not be inserted with the same name as an existing conference" in new WithApplication(FakeApplication()) {
    assertConferencesIsEmpty()
    val t1 = mkQuickConf(0L, "big-ten", "Big Ten")
    Await.result(dao.saveConference(t1), testDbTimeout)
    val t2 = mkQuickConf(0L, "big-10", "Big Ten")
    try {
      Await.result(dao.saveConference(t2), testDbTimeout)
      fail
    } catch {
      case _: Throwable => //OK
    }
  }

  "not be bulk inserted with the same name as an existing confefence" in new WithApplication(FakeApplication()) {
    assertConferencesIsEmpty()
    val ts = List(
      mkQuickConf(0L, "big-east", "Big East"),
      mkQuickConf(0L, "big-ten", "Big Ten"),
      mkQuickConf(0L, "big-10", "Big Ten")
    )
    try {
      Await.result(dao.saveConferences(ts), testDbTimeout)
      fail
    } catch {
      case _: Throwable => //OK
        assertConferencesIsEmpty() //saveConferences is transactional
    }
  }

  "not be updated with the same name as an existing conference" in new WithApplication(FakeApplication()) {
    assertConferencesIsEmpty()
    val t1 = mkQuickConf(0L, "big-ten", "Big Ten")
    Await.result(dao.saveConference(t1), testDbTimeout)
    val t2 = mkQuickConf(0L, "big-east", "Big East")
    val id2 = Await.result(dao.saveConference(t2), testDbTimeout)

    val t3 = t2.copy(id = id2.id, name = "Big Ten")
    try {
      Await.result(dao.saveConference(t3), testDbTimeout)
      fail
    } catch {
      case _: Throwable => //OK
    }
  }

  "find by an id" in new WithApplication(FakeApplication()) {
    assertConferencesIsEmpty()
    private val cs = Await.result(dao.saveConferences(List(
      mkQuickConf(0L, "big-east", "Big East"),
      mkQuickConf(0L, "big-ten", "Big Ten"),
      mkQuickConf(0L, "sec", "Sec")
    )), testDbTimeout)

    assertConferencesSize(3)
     cs.foreach(c=>{
       val rr = Await.result(dao.findConferenceById(c.id), testDbTimeout)
       assert(rr.contains(c))
     })

  }

  "find by a key" in new WithApplication(FakeApplication()) {
    assertConferencesIsEmpty()
    private val cs = Await.result(dao.saveConferences(List(
      mkQuickConf(0L, "big-east", "Big East"),
      mkQuickConf(0L, "big-ten", "Big Ten"),
      mkQuickConf(0L, "sec", "Sec")
    )), testDbTimeout)

    assertConferencesSize(3)
    cs.foreach(c=>{
      val rr = Await.result(dao.findConferenceByKey(c.key), testDbTimeout)
      assert(rr.contains(c))
    })

  }

  "delete by an id" in new WithApplication(FakeApplication()) {
    assertConferencesIsEmpty()
    private val cs = Await.result(dao.saveConferences(List(
      mkQuickConf(0L, "big-east", "Big East"),
      mkQuickConf(0L, "big-ten", "Big Ten"),
      mkQuickConf(0L, "sec", "Sec")
    )), testDbTimeout)

    assertConferencesSize(3)

    val numRows1 = Await.result(dao.deleteConference(cs.head.id), testDbTimeout)
    assert(numRows1 == 1)
    assertConferencesSize(2)
    val rr = Await.result(dao.findConferenceByKey(cs.head.key), testDbTimeout)
    assert(rr.isEmpty)

    val numRows2 = Await.result(dao.deleteConference(-99), testDbTimeout)
    assert(numRows2 == 0)
    assertConferencesSize(2)

  }




def mkQuickConf(id: Long, key: String, name: String): Conference = {
  Conference(id, key, name, None, None, None, None, None, lockRecord = false, LocalDateTime.now(), "Test")
}


  def assertConferencesSize(size: Int) = {
  assert(Await.result(dao.listConferences, testDbTimeout).size == size)
}

  def assertConferencesIsEmpty() = {
  assert(Await.result(dao.listConferences, testDbTimeout).isEmpty, "'Conferences' was not empty.")
}

  def compareConferences(s: Conference, t: Conference) = {
  assert(s.sameData(t), s"Conference $s did not have the same data elements as $t")
}
}
