package com.fijimf.deepfij.model


import cats.effect.{ContextShift, IO}
import com.fijimf.deepfij.auth.model.Password
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import doobie.util.{Colors, ExecutionContexts}
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import org.scalatest._

class PasswordInfoDaoSpec extends FunSuite with Matchers with doobie.scalatest.IOChecker {

  override val colors: Colors.Ansi.type = doobie.util.Colors.Ansi // just for docs
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)
  val transactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:deepfijdb", "fijuser", "mut()mb()"
  )
  private val dao = Password.Dao(transactor)

  test("Test findByLoginInfo") {
    check(dao.findByLoginInfo(new LoginInfo("credentials", "jimf@abc.com")))
  }
  test("Test updateByLoginInfo") {
    check(dao.updateByLoginInfo(new LoginInfo("credentials", "jimf@abc.com"), PasswordInfo("MD5", "33344267&&^&%", None)))
  }
  test("Test insert") {
    check(dao.insert(new LoginInfo("credentials", "jimf@abc.com"),  PasswordInfo( "MD5","33344267&&^&%", None)))
  }

  test("Test upsert") {
    check(dao.upsert(new LoginInfo("credentials", "jimf@abc.com"),  PasswordInfo("MD5","33344267&&^&%", None)))
  }

  test("Test delete") {
    check(dao.delete(new LoginInfo("credentials", "jimf@abc.com")))
  }



}
