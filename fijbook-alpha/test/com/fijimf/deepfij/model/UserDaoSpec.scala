package com.fijimf.deepfij.model

import java.util.UUID

import cats.effect.IO
import com.fijimf.deepfij.model.auth.User
import com.mohiva.play.silhouette.api.LoginInfo
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import org.scalatest._

class UserDaoSpec extends FunSuite with Matchers with doobie.scalatest.IOChecker {

  override val colors = doobie.util.Colors.Ansi // just for docs
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)
  val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:deepfijdb", "fijuser", "mut()mb()"
  )
  private val dao = User.Dao(transactor)
  test("Test findById") {
    check(dao.findById(UUID.randomUUID()))
  }
  test("Test findByLoginInfo") {
    check(dao.findByLoginInfo(new LoginInfo("credentials", "jimf@abc.com")))
  }
  test("Test list") {
    check(dao.list)
  }
  test("Test save") {
    check(dao.save(User(UUID.randomUUID(), "credentials", "a@bc.com", None, None, Some("Jim F."), Some("a@bc.com"), None, activated = true)))
  }


}
