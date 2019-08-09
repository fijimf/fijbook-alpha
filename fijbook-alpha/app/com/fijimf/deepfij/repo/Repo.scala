package com.fijimf.deepfij.repo

import doobie._
import doobie.implicits._
import cats._
import cats.effect._
import cats.implicits._
import com.fijimf.deepfij.model.auth
import com.fijimf.deepfij.model.schedule._
import com.fijimf.deepfij.model.auth._
import doobie.util.transactor.Transactor.Aux
import org.postgresql.Driver
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
      PasswordInfo.Dao(xa)
    )
    (daos.map(_.dropDdl)++daos.map(_.createDdl)).sequence
  }

  def main(args: Array[String]): Unit = {
    val program1 = sql"select 42".query[Int].unique // 42.pure[ConnectionIO]
    import doobie.util.ExecutionContexts
    implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

    val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",           // driver classname
      "jdbc:postgresql://localhost:5432/deepfijdb",    // connect URL (driver-specific)
      "fijuser",                          // user
      "mut()mb()",                        // password
      ExecutionContexts.synchronous // just for testing
    )
//    println(program1.transact(xa).unsafeRunSync())
//
//    println(Season.Dao(xa).saveSeason(Season(0L,2019)).transact(xa).unsafeRunSync())

    createScheduleTables(xa).transact(xa).unsafeRunSync()
  }

}
