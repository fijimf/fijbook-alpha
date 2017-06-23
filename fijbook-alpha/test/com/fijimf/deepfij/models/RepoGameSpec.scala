package com.fijimf.deepfij.models

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration._


class RepoGameSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach  with RebuildDatabaseMixin with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]

  /*
  def listGames: Future[List[Game]]
  def deleteGames(ids: List[Long]):Future[Unit]
  def upsertGame(game: Game): Future[Long]
  def clearGamesByDate(d: LocalDate): Future[Int]
  def saveGame(gt: (Game, Option[Result])): Future[Long]
  def gamesByDate(d: List[LocalDate]): Future[List[(Game, Option[Result])]]
  def gamesBySource(sourceKey: String): Future[List[(Game, Option[Result])]]
   */

  "Games " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assertGamesIsEmpty
    }
  }


  private def assertGamesIsEmpty = {
    assert(Await.result(dao.listGames, testDbTimeout).isEmpty)
  }
}
