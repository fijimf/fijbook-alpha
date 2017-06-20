package com.fijimf.deepfij.models

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await


class RepoAliasSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]

  "Aliases " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listAliases, testDbTimeout).isEmpty)
    }

    "allow an alias to be saved"in new WithApplication(FakeApplication()) {
      private val toBeSaved = Alias(0L, "W. Virginia", "West Virginia")
      private val id = Await.result(dao.saveAlias(toBeSaved), testDbTimeout).id
      assert(id>0)
      private val aliases = Await.result(dao.listAliases, testDbTimeout)
      assert(aliases.size==1)
      assert(aliases.head==toBeSaved.copy(id=id))
    }

    "allow an alias to be deleted" in new WithApplication(FakeApplication()) {
      private val idToDelete = Await.result(dao.saveAlias(Alias(0L, "W. Virginia", "West Virginia")), testDbTimeout).id
      assert(idToDelete>0)
      private val before = Await.result(dao.listAliases, testDbTimeout)
      assert(before.size==1)
      Await.ready(dao.deleteAlias(idToDelete), testDbTimeout)
      private val after = Await.result(dao.listAliases, testDbTimeout)
      assert(after.isEmpty)

    }

    "find an alias by id "in new WithApplication(FakeApplication()){
      Await.result(dao.saveAlias(Alias(0L, "W. Virginia", "West Virginia")), testDbTimeout)
      val armyId=Await.result(dao.saveAlias(Alias(0L, "Army", "West Point")), testDbTimeout).id
      Await.result(dao.saveAlias(Alias(0L, "UVA", "Virginia")), testDbTimeout)
      private val before = Await.result(dao.listAliases, testDbTimeout)
      assert(before.size==3)
      Await.result(dao.findAliasById(armyId), testDbTimeout).map(_.id) match {
        case Some(al)=>assert(al==armyId)
        case None=> fail()
      }


    }

    "delete all aliases "in new WithApplication(FakeApplication()){
     Await.result(dao.saveAlias(Alias(0L, "W. Virginia", "West Virginia")), testDbTimeout)
     Await.result(dao.saveAlias(Alias(0L, "Army", "West Point")), testDbTimeout)
     Await.result(dao.saveAlias(Alias(0L, "UVA", "Virginia")), testDbTimeout)
      private val before = Await.result(dao.listAliases, testDbTimeout)
      assert(before.size==3)
      Await.ready(dao.deleteAliases(), testDbTimeout)
      private val after = Await.result(dao.listAliases, testDbTimeout)
      assert(after.isEmpty)

    }

  }


}
