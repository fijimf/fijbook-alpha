package com.fijimf.deepfij.models

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}


class RepoGamePredictionSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach  with RebuildDatabaseMixin with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]

  import scala.concurrent.ExecutionContext.Implicits.global



  "GamePredictions " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listGamePrediction, testDbTimeout).isEmpty)
    }

    "can save a game prediction" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listGamePrediction, testDbTimeout).isEmpty)

      private val predictions = List(GamePrediction(0L, 123L, "My Model", Some(123L), Some(0.75), None, None))
      Await.result(dao.saveGamePredictions(predictions).andThen {
        case Success(lst) =>
          assert(lst.size == 1)
          assert(lst(0) > 0)
        case Failure(ex) => fail("Unexpected exception", ex)
      }, 30.seconds)
    }
    "can save multiple a game predictions" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listGamePrediction, testDbTimeout).isEmpty)

      private val predictions = List(
        GamePrediction(0L, 123L, "My Model", Some(3), Some(0.75), None, None),
        GamePrediction(0L, 124L, "My Model", Some(4), Some(0.75), None, None),
        GamePrediction(0L, 125L, "My Model", Some(5), Some(0.75), None, None),
        GamePrediction(0L, 456L, "My Model", Some(6), Some(0.75), None, None),
        GamePrediction(0L, 457L, "My Model", Some(7), Some(0.75), None, None),
        GamePrediction(0L, 458L, "My Model", Some(8), Some(0.75), None, None),
        GamePrediction(0L, 123L, "My Other Model", Some(3), Some(0.75), None, None),
        GamePrediction(0L, 124L, "My Other Model", Some(4), Some(0.75), None, None),
        GamePrediction(0L, 125L, "My Other Model", Some(5), Some(0.75), None, None),
        GamePrediction(0L, 456L, "My Other Model", Some(6), Some(0.75), None, None),
        GamePrediction(0L, 457L, "My Other Model", Some(7), Some(0.75), None, None),
        GamePrediction(0L, 458L, "My Other Model", Some(8), Some(0.75), None, None)

      )
      Await.result(dao.saveGamePredictions(predictions).andThen {
        case Success(lst) =>
          assert(lst.size == 12)
          assert(lst.forall(_ > 0))
        case Failure(ex) => fail("Unexpected exception", ex)
      }, 30.seconds)
    }
    "can load game predictions for a model" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listGamePrediction, testDbTimeout).isEmpty)

      private val predictions = List(
        GamePrediction(0L, 123L, "My Model", Some(3), Some(0.75), None, None),
        GamePrediction(0L, 124L, "My Model", Some(4), Some(0.75), None, None),
        GamePrediction(0L, 125L, "My Model", Some(5), Some(0.75), None, None),
        GamePrediction(0L, 456L, "My Model", Some(6), Some(0.75), None, None),
        GamePrediction(0L, 457L, "My Model", Some(7), Some(0.75), None, None),
        GamePrediction(0L, 458L, "My Model", Some(8), Some(0.75), None, None),
        GamePrediction(0L, 123L, "My Other Model", Some(3), Some(0.75), None, None),
        GamePrediction(0L, 124L, "My Other Model", Some(4), Some(0.75), None, None),
        GamePrediction(0L, 125L, "My Other Model", Some(5), Some(0.75), None, None),
        GamePrediction(0L, 456L, "My Other Model", Some(6), Some(0.75), None, None),
        GamePrediction(0L, 457L, "My Other Model", Some(7), Some(0.75), None, None),
        GamePrediction(0L, 458L, "My Other Model", Some(8), Some(0.75), None, None)

      )

      val g3 = Game(123L, 1L, 3L, 33L, LocalDate.now(), LocalDateTime.now(), None, false, None, None, None, "xx", LocalDateTime.now(), "me")
      val g4 = Game(124L, 1L, 4L, 34L, LocalDate.now(), LocalDateTime.now(), None, false, None, None, None, "xx", LocalDateTime.now(), "me")
      val g5 = Game(125L, 1L, 5L, 35L, LocalDate.now(), LocalDateTime.now(), None, false, None, None, None, "xx", LocalDateTime.now(), "me")
      val g6 = Game(456L, 1L, 6L, 36L, LocalDate.now(), LocalDateTime.now(), None, false, None, None, None, "xx", LocalDateTime.now(), "me")
      val g7 = Game(457L, 1L, 7L, 37L, LocalDate.now(), LocalDateTime.now(), None, false, None, None, None, "xx", LocalDateTime.now(), "me")
      val g8 = Game(458L, 1L, 8L, 38L, LocalDate.now(), LocalDateTime.now(), None, false, None, None, None, "xx", LocalDateTime.now(), "me")
      val g9 = Game(999L, 1L, 9L, 99L, LocalDate.now(), LocalDateTime.now(), None, false, None, None, None, "xx", LocalDateTime.now(), "me")
      Await.result(dao.saveGamePredictions(predictions).andThen {
        case Success(lst) =>
          assert(lst.size == 12)
          assert(lst.forall(_ > 0))
        case Failure(ex) => fail("Unexpected exception", ex)
      }, 30.seconds)
      Await.result(dao.loadGamePredictions(List(g3, g4, g5), "My Model").andThen {
        case Success(lst) =>
          assert(lst.size == 3)
          assert(lst.forall(_.id > 0))
        case Failure(ex) => fail("Unexpected exception", ex)
      }, 30.seconds)
      Await.result(dao.loadGamePredictions(List(g6, g7, g8), "My Other Model").andThen {
        case Success(lst) =>
          assert(lst.size == 3)
          assert(lst.forall(_.id > 0))
        case Failure(ex) => fail("Unexpected exception", ex)
      }, 30.seconds)
      Await.result(dao.loadGamePredictions(List(g9), "My Other Model").andThen {
        case Success(lst) =>
          assert(lst.isEmpty)
        case Failure(ex) => fail("Unexpected exception", ex)
      }, 30.seconds)
      Await.result(dao.loadGamePredictions(List(g3), "My Nonexistent Model").andThen {
        case Success(lst) =>
          assert(lst.isEmpty)
        case Failure(ex) => fail("Unexpected exception", ex)
      }, 30.seconds)


    }
  }


}
