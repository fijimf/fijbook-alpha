package com.fijimf.deepfij.models

import java.time.LocalDate

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.{Await, Future}

class RepoXStatSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin {

  import scala.concurrent.ExecutionContext.Implicits.global

  val dao: ScheduleDAO = Injector.inject[ScheduleDAO]

  val thirtyDays: List[LocalDate] = 1.to(30).map(d => LocalDate.now().plusDays(d)).toList
  val twoDays: List[LocalDate] = thirtyDays.take(2)

  val statKeys: List[String] = List("s-1", "s-2")

  val teamIds: List[Int] = 1.to(350).toList
  val shortTeamIds: List[Int] = teamIds.take(10)

  "XStats" should {
    "be empty initially" in new WithApplication() {
      assertXStatsIsEmpty()
    }

    "save a single new xstat" in new WithApplication() {
      assertXStatsIsEmpty()
      private val x = XStat(
        id = 0L,
        seasonId = 1L,
        date = LocalDate.now(),
        key = "N",
        teamId = 1L,
        value = Some(1.0),
        rank = Some(1),
        percentile = Some(.99),
        mean = Some(100),
        stdDev = Some(1.2),
        min = Some(0.0),
        max = Some(1.0),
        n = 1
      )
      Await.ready(for {
        _ <- dao.saveXStat(x)
        lx <- dao.listXStats
      } yield {
        assert(lx.size == 1)
        val x1 = lx.head
        assert(x1.id > 0L)
        assert(x1.seasonId === x.seasonId)
        assert(x1.date === x.date)
        assert(x1.key === x.key)
        assert(x1.teamId === x.teamId)
        assert(x1.value === x.value)
        assert(x1.rank === x.rank)
        assert(x1.percentile === x.percentile)
        assert(x1.mean === x.mean)
        assert(x1.stdDev === x.stdDev)
        assert(x1.min === x.min)
        assert(x1.max === x.max)
        assert(x1.n === x.n)
      }, testDbTimeout)
    }

    "save a single new xstat with missing data" in new WithApplication() {
      assertXStatsIsEmpty()
      private val x = XStat(
        id = 0L,
        seasonId = 1L,
        date = LocalDate.now(),
        key = "N",
        teamId = 1L,
        value = None,
        rank = None,
        percentile = None,
        mean = None,
        stdDev = None,
        min = None,
        max = None,
        n = 1
      )
      Await.ready(for {
        _ <- dao.saveXStat(x)
        lx <- dao.listXStats
      } yield {
        assert(lx.size == 1)
        val x1 = lx.head
        assert(x1.id > 0L)
        assert(x1.seasonId === x.seasonId)
        assert(x1.date === x.date)
        assert(x1.key === x.key)
        assert(x1.teamId === x.teamId)
        assert(x1.value === x.value)
        assert(x1.rank === x.rank)
        assert(x1.percentile === x.percentile)
        assert(x1.mean === x.mean)
        assert(x1.stdDev === x.stdDev)
        assert(x1.min === x.min)
        assert(x1.max === x.max)
        assert(x1.n === x.n)
      }, testDbTimeout)
    }

    "update a single new xstat" in new WithApplication() {
      assertXStatsIsEmpty()
      private val x = XStat(
        id = 0L,
        seasonId = 1L,
        date = LocalDate.now(),
        key = "N",
        teamId = 1L,
        value = Some(1.0),
        rank = Some(1),
        percentile = Some(.99),
        mean = Some(100),
        stdDev = Some(1.2),
        min = Some(0.0),
        max = Some(1.0),
        n = 1
      )
      Await.ready(for {
        _ <- dao.saveXStat(x)
        lx <- dao.listXStats
        _ <- dao.saveXStat(lx.head.copy(value = Some(9999.0), percentile = Some(0.001)))
        ly <- dao.listXStats
      } yield {
        assert(ly.size == 1)
        val y1 = ly.head
        assert(y1.id > 0L)
        assert(y1.seasonId === x.seasonId)
        assert(y1.date === x.date)
        assert(y1.key === x.key)
        assert(y1.teamId === x.teamId)
        assert(y1.value === Some(9999.0))
        assert(y1.rank === x.rank)
        assert(y1.percentile === Some(0.001))
        assert(y1.mean === x.mean)
        assert(y1.stdDev === x.stdDev)
        assert(y1.min === x.min)
        assert(y1.max === x.max)
        assert(y1.n === x.n)
      }, testDbTimeout)
    }

    "update a single new xstat from missing data" in new WithApplication() {
      assertXStatsIsEmpty()
      private val x = XStat(
        id = 0L,
        seasonId = 1L,
        date = LocalDate.now(),
        key = "N",
        teamId = 1L,
        value = None,
        rank = None,
        percentile = None,
        mean = None,
        stdDev = None,
        min = None,
        max = None,
        n = 1
      )
      Await.ready(for {
        _ <- dao.saveXStat(x)
        lx <- dao.listXStats
        _ <- dao.saveXStat(lx.head.copy(value = Some(9999.0), percentile = Some(0.001)))
        ly <- dao.listXStats
      } yield {
        assert(ly.size == 1)
        val y1 = ly.head
        assert(y1.id > 0L)
        assert(y1.seasonId === x.seasonId)
        assert(y1.date === x.date)
        assert(y1.key === x.key)
        assert(y1.teamId === x.teamId)
        assert(y1.value === Some(9999.0))
        assert(y1.rank === x.rank)
        assert(y1.percentile === Some(0.001))
        assert(y1.mean === x.mean)
        assert(y1.stdDev === x.stdDev)
        assert(y1.min === x.min)
        assert(y1.max === x.max)
        assert(y1.n === x.n)
      }, testDbTimeout)
    }


    "update a single new xstat to missing data" in new WithApplication() {
      assertXStatsIsEmpty()
      private val x = XStat(
        id = 0L,
        seasonId = 1L,
        date = LocalDate.now(),
        key = "N",
        teamId = 1L,
        value = Some(1.0),
        rank = Some(1),
        percentile = Some(.99),
        mean = Some(100),
        stdDev = Some(1.2),
        min = Some(0.0),
        max = Some(1.0),
        n = 1
      )
      Await.ready(for {
        _ <- dao.saveXStat(x)
        lx <- dao.listXStats
        _ <- dao.saveXStat(lx.head.copy(value = None, percentile = None))
        ly <- dao.listXStats
      } yield {
        assert(ly.size == 1)
        val y1 = ly.head
        assert(y1.id > 0L)
        assert(y1.seasonId === x.seasonId)
        assert(y1.date === x.date)
        assert(y1.key === x.key)
        assert(y1.teamId === x.teamId)
        assert(y1.value === None)
        assert(y1.rank === x.rank)
        assert(y1.percentile === None)
        assert(y1.mean === x.mean)
        assert(y1.stdDev === x.stdDev)
        assert(y1.min === x.min)
        assert(y1.max === x.max)
        assert(y1.n === x.n)
      }, testDbTimeout)
    }

    "save a small list of xstats" in new WithApplication() {
      assertXStatsIsEmpty()
      private val stats: List[XStat] = for {
        d <- twoDays
        s <- statKeys
        t <- shortTeamIds
      } yield {
        XStat(
          id = 0L,
          seasonId = 1L,
          date = d,
          key = s,
          teamId = t.toLong,
          value = Some(1.0),
          rank = Some(1),
          percentile = Some(.99),
          mean = Some(100),
          stdDev = Some(1.2),
          min = Some(0.0),
          max = Some(1.0),
          n = shortTeamIds.size
        )
      }
      Await.ready(for {
        _ <- dao.saveXStats(stats)
        lx <- dao.listXStats
      } yield {
        assert(lx.size === stats.size)
        lx.foreach(x => {
          assert(x.id > 0L)
          assert(x.value === Some(1.0))
        })
      }, testDbTimeout)
    }

    "update a small list of xstats" in new WithApplication() {
      assertXStatsIsEmpty()
      private val stats: List[XStat] = for {
        d <- twoDays
        s <- statKeys
        t <- shortTeamIds
      } yield {
        XStat(
          id = 0L,
          seasonId = 1L,
          date = d,
          key = s,
          teamId = t.toLong,
          value = Some(1.0),
          rank = Some(1),
          percentile = Some(.99),
          mean = Some(100),
          stdDev = Some(1.2),
          min = Some(0.0),
          max = Some(1.0),
          n = shortTeamIds.size
        )
      }
      Await.ready(for {
        _ <- dao.saveXStats(stats)
        lx <- dao.listXStats
        _ <- dao.saveXStats(lx.map(x => {
          if (x.teamId % 2 == 0) x.copy(value = Some(99.99)) else x
        }))
        ly <- dao.listXStats
      } yield {
        assert(ly.size === stats.size)
        ly.foreach(y => {
          assert(y.id > 0L)
          if (y.teamId % 2 == 0)
            assert(y.value === Some(99.99))
          else
            assert(y.value === Some(1.0))
        })
      }, testDbTimeout)
    }

    "update a small list of xstats ensuring its by compound key not ID" in new WithApplication() {
      assertXStatsIsEmpty()
      private val stats: List[XStat] = for {
        d <- twoDays
        s <- statKeys
        t <- shortTeamIds
      } yield {
        XStat(
          id = 0L,
          seasonId = 1L,
          date = d,
          key = s,
          teamId = t.toLong,
          value = Some(1.0),
          rank = Some(1),
          percentile = Some(.99),
          mean = Some(100),
          stdDev = Some(1.2),
          min = Some(0.0),
          max = Some(1.0),
          n = shortTeamIds.size
        )
      }
      Await.ready(for {
        _ <- dao.saveXStats(stats)
        lx <- dao.listXStats
        _ <- dao.saveXStats(lx.map(x => {
          if (x.teamId % 2 == 0) x.copy(id = 0L, value = Some(99.99)) else x.copy(id = 0L)
        }))
        ly <- dao.listXStats
      } yield {
        assert(ly.size === stats.size)
        ly.foreach(y => {
          assert(y.id > 0L)
          if (y.teamId % 2 == 0)
            assert(y.value === Some(99.99))
          else
            assert(y.value === Some(1.0))
        })
      }, testDbTimeout)
    }


    "be able to save large statValues" in new WithApplication() {
      assertXStatsIsEmpty()
      private val statValues = 1.to(200).flatMap(t =>
        thirtyDays.flatMap(d =>
          List("Stat1", "Stat2").map(st =>
            XStat(0L, 1L, d, st, t.toLong, Some(1.0), Some(1), Some(.99), Some(100), Some(1.2), Some(0.0), Some(1.0), 2)))).toList
      Await.result(dao.saveXStats(statValues), testDbTimeout)
      assert(Await.result(dao.listXStats, testDbTimeout).size == 30 * 200 * 2)
    }

    "be able to save then update statValues" in new WithApplication() {
      assertXStatsIsEmpty()
      private val statValues = 1.to(200).flatMap(t =>
        thirtyDays.flatMap(d =>
          List("Stat1", "Stat2").map(st =>
            XStat(0L, 1L, d, st, t.toLong, Some(1.0), Some(1), Some(.99), Some(100), Some(1.2), Some(0.0), Some(1.0), 2)))).toList
      Await.result(dao.saveXStats(statValues), testDbTimeout)
      Await.result(dao.saveXStats(statValues), testDbTimeout)
      assert(Await.result(dao.listXStats, testDbTimeout).size == 30 * 200 * 2)
    }

    "be able to save large batched statValues sequentially" in new WithApplication() {
      assertXStatsIsEmpty()
      private val statValues: List[List[XStat]] =
        1.to(200).flatMap(t =>
          thirtyDays.map(d =>
            List("Stat1", "Stat2").map(st =>
              XStat(0L, 1L, d, st, t.toLong, Some(1.0), Some(1), Some(.99), Some(100), Some(1.2), Some(0.0), Some(1.0), 2)).toList
          )).toList

      val zz = statValues.foldLeft(Future(0)) { case (i: Future[Int], stats: List[XStat]) => i.flatMap(n => {
        dao.saveXStats(stats).map(_.sum).map(_ + n)
      })
      }
      Await.result(zz, testDbTimeout)
      assert(Await.result(dao.listXStats, testDbTimeout).size == 30 * 200 * 2)
    }

    "be able to save large batched statValues concurrently" in new WithApplication() {
      assertXStatsIsEmpty()
      private val statValues: List[List[XStat]] =
        1.to(200).flatMap(t =>
          thirtyDays.map(d =>
            List("Stat1", "Stat2").map(st =>
              XStat(0L, 1L, d, st, t.toLong, Some(1.0), Some(1), Some(.99), Some(100), Some(1.2), Some(0.0), Some(1.0), 2)).toList
          )).toList

      val zz = Future.sequence(statValues.map(stats=> dao.saveXStats(stats)))
      Await.result(zz, testDbTimeout)
      assert(Await.result(dao.listXStats, testDbTimeout).size == 30 * 200 * 2)
    }
  }

  private def assertXStatsIsEmpty() = {
    assert(Await.result(dao.listXStats, testDbTimeout).isEmpty)
  }
}

