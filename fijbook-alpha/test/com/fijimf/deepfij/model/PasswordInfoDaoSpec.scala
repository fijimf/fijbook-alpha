package com.fijimf.deepfij.model

import java.util.UUID

import cats.effect.IO
import com.fijimf.deepfij.model.auth.{PasswordInfo, User}
import com.mohiva.play.silhouette.api.LoginInfo
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import org.scalatest._

class PasswordInfoDaoSpec extends FunSuite with Matchers with doobie.scalatest.IOChecker {

  override val colors = doobie.util.Colors.Ansi // just for docs
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)
  val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:deepfijdb", "fijuser", "mut()mb()"
  )
  private val dao = PasswordInfo.Dao(transactor)

  test("Test findByLoginInfo") {
    check(dao.findByLoginInfo(new LoginInfo("credentials", "jimf@abc.com")))
  }
  test("Test updateByLoginInfo") {
    check(dao.updateByLoginInfo(PasswordInfo(0L, "MD5","33344267&&^&%", None, "credentials","fijimf@gmail.com")))
  }
  test("Test insert") {
    check(dao.insert(PasswordInfo(0L, "MD5","33344267&&^&%", None, "credentials","fijimf@gmail.com")))
  }

  test("Test upsert") {
    check(dao.upsert((PasswordInfo(0L, "MD5","33344267&&^&%", None, "credentials","fijimf@gmail.com"))))
  }

  test("Test delete") {
    check(dao.delete(new LoginInfo("credentials", "jimf@abc.com")))
  }



}
