package com.fijimf.deepfij

import cats.effect.{Bracket, ContextShift, IO}
import cats.implicits._
import com.fijimf.deepfij.auth.model.User
import com.fijimf.deepfij.auth.services.PasswordOps
import com.fijimf.deepfij.schedule.model._
import doobie.Transactor
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux

object Repo {

  def createScheduleTables[M[_]](xa:Transactor[M])(implicit M: Bracket[M, Throwable]): doobie.ConnectionIO[List[Int]] = {
    val daos = List(
      Alias.Dao(xa),
      Conference.Dao(xa),
      ConferenceMap.Dao(xa),
      Game.Dao(xa),
      Quote.Dao(xa),
      Result.Dao(xa),
      Season.Dao(xa),
      Team.Dao(xa),
      TournamentData.Dao(xa),
      User.Dao(xa),
      PasswordOps.Dao(xa)
    )
    (daos.map(_.dropDdl)++daos.map(_.createDdl)).sequence
  }

  def main(args: Array[String]): Unit = {
    import doobie.util.ExecutionContexts
    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

    val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",           // driver classname
      "jdbc:postgresql://localhost:5432/deepfijdb",    // connect URL (driver-specific)
      "fijuser",                          // user
      "mut()mb()",                        // password
      ExecutionContexts.synchronous // just for testing
    )
    createScheduleTables(xa).transact(xa).unsafeRunSync()
  }

}
