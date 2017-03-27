package com.fijimf.deepfij.models

import org.scalatest.BeforeAndAfterEach
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration.Duration


trait RebuildDatabaseMixin {
  self: BeforeAndAfterEach =>
  val repo = Injector.inject[ScheduleRepository]

  override def beforeEach() = {
    Await.result(repo.createSchema(), Duration.Inf)
  }

  override def afterEach() = {
    Await.result(repo.dropSchema(), Duration.Inf)
  }

}
