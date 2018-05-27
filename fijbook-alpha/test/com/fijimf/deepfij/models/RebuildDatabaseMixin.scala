package com.fijimf.deepfij.models

import org.scalatest.BeforeAndAfterEach
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

trait RebuildDatabaseMixin {
  self: BeforeAndAfterEach =>
  val repo = Injector.inject[ScheduleRepository]

  val testDbTimeout = 20.seconds

  override def beforeEach() = {
//    import slick.jdbc.H2Profile.api._
//    Await.result(repo.db.run(sqlu"DROP ALL OBJECTS"), Duration.Inf)
    Await.result(repo.createSchema(), Duration.Inf)
  }

  override def afterEach() = {
    Await.result(repo.dropSchema(), Duration.Inf)
  }

}

