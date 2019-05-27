package com.fijimf.deepfij.models

import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await


class RepoFavoriteLinkSpec extends PlaySpec with GuiceOneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {
  val dao = Injector.inject[ScheduleDAO]

  "FavoriteLinks " should {
    "be empty initially" in new WithApplication() {
      assert(Await.result(dao.listFavoriteLinks, testDbTimeout).isEmpty)
    }

    "be able to be inserted" in new WithApplication() {
      val f = FavoriteLink(0L, "fijimf@gmail.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now())
      val g = Await.result(dao.saveFavoriteLink(f), testDbTimeout)
      assert(g.id > 0)
      assert(Await.result(dao.listFavoriteLinks, testDbTimeout).size === 1)
    }
    "be able to be inserted in bulk" in new WithApplication() {
      val fs = List(
        FavoriteLink(0L, "fijimf@gmail.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Gonzaga", "/team/gonzaga", 2, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Regression", "/stat/regression", 3, LocalDateTime.now()),
        FavoriteLink(0L, "rickey55@yahoo.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now())
      )
      private val links: List[FavoriteLink] = Await.result(dao.saveFavoriteLinks(fs), testDbTimeout)
      links.foreach(fl => assert(fl.id > 0))
      assert(Await.result(dao.listFavoriteLinks, testDbTimeout).size === 4)
    }

    "be able to be updated individually " in new WithApplication() {
      val fs = List(
        FavoriteLink(0L, "fijimf@gmail.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Gonzaga", "/team/gonzaga", 2, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Regression", "/stat/regression", 3, LocalDateTime.now()),
        FavoriteLink(0L, "rickey55@yahoo.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now())
      )
      private val links: List[FavoriteLink] = Await.result(dao.saveFavoriteLinks(fs), testDbTimeout)
      links.foreach(fl => assert(fl.id > 0))
      assert(Await.result(dao.listFavoriteLinks, testDbTimeout).size === 4)

      val first: FavoriteLink = links.head
      val f: FavoriteLink = Await.result(dao.saveFavoriteLink(first.copy(order = 99)), testDbTimeout)
      assert(f.id === first.id)
      assert(f.order != first.order)
      assert(f.order === 99)

      private val ww: List[FavoriteLink] = Await.result(dao.listFavoriteLinks, testDbTimeout)
      assert(ww.size === 4)
      assert(ww.count(_.order === 99) === 1)

    }

    "be able to be updated in bulk" in new WithApplication() {
      val fs: List[FavoriteLink] = List(
        FavoriteLink(0L, "fijimf@gmail.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Gonzaga", "/team/gonzaga", 2, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Regression", "/stat/regression", 3, LocalDateTime.now()),
        FavoriteLink(0L, "rickey55@yahoo.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now())
      )
      val links: List[FavoriteLink] = Await.result(dao.saveFavoriteLinks(fs), testDbTimeout)
      links.foreach(fl => assert(fl.id > 0))
      assert(Await.result(dao.listFavoriteLinks, testDbTimeout).size === 4)

      val moddedLinks: List[FavoriteLink] = links.map(lk => lk.copy(order = lk.order + 99))
      val gs: List[FavoriteLink] = Await.result(dao.saveFavoriteLinks(moddedLinks), testDbTimeout)
      //TODO add asserts
    }

    "be able to mix updates and inserts" in new WithApplication() {
      val fs: List[FavoriteLink] = List(
        FavoriteLink(0L, "fijimf@gmail.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Gonzaga", "/team/gonzaga", 2, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Regression", "/stat/regression", 3, LocalDateTime.now()),
        FavoriteLink(0L, "rickey55@yahoo.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now())
      )
      val links: List[FavoriteLink] = Await.result(dao.saveFavoriteLinks(fs), testDbTimeout)
      links.foreach(fl => assert(fl.id > 0))
      assert(Await.result(dao.listFavoriteLinks, testDbTimeout).size === 4)

      val moddedLinks: List[FavoriteLink] = links.map(lk => lk.copy(order = lk.order + 99))

      val otherLinks = List(
        FavoriteLink(0L, "rickey55@yahoo.com", "Gonzaga", "/team/gonzaga", 2, LocalDateTime.now()),
        FavoriteLink(0L, "rickey55@yahoo.com", "Regression", "/stat/regression", 3, LocalDateTime.now())
      )

      val gs: List[FavoriteLink] = Await.result(dao.saveFavoriteLinks(moddedLinks++otherLinks), testDbTimeout)

      gs.foreach(g=>assert(g.id>0L))

    }

    "be able to be retrieved by user" in new WithApplication() {

      val fs: List[FavoriteLink] = List(
        FavoriteLink(0L, "fijimf@gmail.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Gonzaga", "/team/gonzaga", 2, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Regression", "/stat/regression", 3, LocalDateTime.now()),
        FavoriteLink(0L, "rickey55@yahoo.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now())
      )
      Await.result(dao.saveFavoriteLinks(fs), testDbTimeout)

      val fijimf: List[FavoriteLink] = Await.result(dao.findFavoriteLinksByUser("fijimf@gmail.com"), testDbTimeout)
      assert(fijimf.length === 3)
      assert(fijimf.map(_.displayAs).toSet === Set("Georgetown", "Gonzaga", "Regression"))
      val zeke: List[FavoriteLink] = Await.result(dao.findFavoriteLinksByUser("rickey55@yahoo.com"), testDbTimeout)
      assert(zeke.length === 1)
      assert(zeke.map(_.displayAs).toSet === Set("Georgetown"))

    }
    "be able to be retrieved by page" in new WithApplication() {

      val fs: List[FavoriteLink] = List(
        FavoriteLink(0L, "fijimf@gmail.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Gonzaga", "/team/gonzaga", 2, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Regression", "/stat/regression", 3, LocalDateTime.now()),
        FavoriteLink(0L, "rickey55@yahoo.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now())
      )
      Await.result(dao.saveFavoriteLinks(fs), testDbTimeout)

      val hoyas: List[FavoriteLink] = Await.result(dao.findFavoriteLinksByPage("/team/georgetown"), testDbTimeout)
      assert(hoyas.length === 2)
      assert(hoyas.map(_.displayAs).toSet === Set("Georgetown"))
      val zags: List[FavoriteLink] = Await.result(dao.findFavoriteLinksByPage("/team/gonzaga"), testDbTimeout)
      assert(zags.length === 1)
      assert(zags.map(_.displayAs).toSet === Set("Gonzaga"))


    }

    "be able to be all deleted" in new WithApplication() {

      val fs: List[FavoriteLink] = List(
        FavoriteLink(0L, "fijimf@gmail.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Gonzaga", "/team/gonzaga", 2, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Regression", "/stat/regression", 3, LocalDateTime.now()),
        FavoriteLink(0L, "rickey55@yahoo.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now())
      )
      Await.result(dao.saveFavoriteLinks(fs), testDbTimeout)
      Await.ready(dao.deleteAllFavoriteLinks(), testDbTimeout)
      assert(Await.result(dao.listFavoriteLinks, testDbTimeout).isEmpty)
    }
    "be able to be deleted by user" in new WithApplication() {
      val fs: List[FavoriteLink] = List(
        FavoriteLink(0L, "fijimf@gmail.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Gonzaga", "/team/gonzaga", 2, LocalDateTime.now()),
        FavoriteLink(0L, "fijimf@gmail.com", "Regression", "/stat/regression", 3, LocalDateTime.now()),
        FavoriteLink(0L, "rickey55@yahoo.com", "Georgetown", "/team/georgetown", 1, LocalDateTime.now())
      )
      Await.result(dao.saveFavoriteLinks(fs), testDbTimeout)
      Await.ready(dao.deleteUsersFavoriteLinks("fijimf@gmail.com"), testDbTimeout)
      assert(Await.result(dao.listFavoriteLinks, testDbTimeout).size === 1)

    }

  }


}
