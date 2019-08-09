package com.fijimf.deepfij.model

import java.util.UUID

import cats.effect.IO
import com.fijimf.deepfij.model.auth.{PasswordInfo, User}
import com.fijimf.deepfij.model.schedule._
import doobie.util.transactor.Transactor
import doobie.util.{ExecutionContexts, Put, Read}
import org.scalatest._

class ModelDaoSpec extends FunSuite with Matchers with doobie.scalatest.IOChecker {

  override val colors = doobie.util.Colors.Ansi // just for docs
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)
  val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:deepfijdb", "fijuser", "mut()mb()"
  )
import ModelDao._
  checkModelDao[Alias, Long](Alias.Dao(transactor), "Alias", 0L)
  checkModelDao[Conference, Long](Conference.Dao(transactor), "Conference", 0L)
  checkModelDao[ConferenceMap, Long](ConferenceMap.Dao(transactor), "ConferenceMap", 0L)
  checkModelDao[Quote, Long](Quote.Dao(transactor), "Quote", 0L)
  checkModelDao[Result, Long](Result.Dao(transactor), "Result", 0L)
  checkModelDao[Season, Long](Season.Dao(transactor), "Season", 0L)
  checkModelDao[Team, Long](Team.Dao(transactor), "Team", 0L)
  checkModelDao[TournamentData, Long](TournamentData.Dao(transactor), "TournamentData", 0L)
  checkModelDao[Game, Long](Game.Dao(transactor), "Game", 0L)
  checkModelDao[User, UUID](User.Dao(transactor), "User", UUID.randomUUID())
  checkModelDao[PasswordInfo, Long](PasswordInfo.Dao(transactor), "PasswordInfo", 0L)


  def checkModelDao[K, ID](dao: ModelDao[K, ID], name: String, id: ID)(implicit ID: Put[ID], K: Read[K]): Unit = {
    test(s"$name select all") {
      check(dao.select.query[K])
    }

    test(s"$name delete all") {
      check(dao.delete.update)
    }

    test(s"$name select by id") {
      check((dao.select ++ dao.idPredicate(id)).query[K])
    }

    test(s"$name delete by id ") {
      check((dao.delete ++ dao.idPredicate(id)).update)
    }

  }
}
