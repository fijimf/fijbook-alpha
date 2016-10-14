package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.{ScheduleRepository, UserRepository}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller, Result}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SchemaHelper @Inject()(repo: ScheduleRepository, userRepo: UserRepository) extends Controller {

  def dumpSchema() = Action.async { implicit rs =>
    repo.dumpSchema().map(tup =>Ok (views.html.admin.show_schema(tup)))
  }

  def dropCreateSchema() = Action.async { implicit rs =>
    for (
      u1 <- repo.dropSchema();
      u2 <- repo.createSchema()
    ) yield {
      Redirect("/deepfij/admin").flashing("level"->"info", "message"->"Schedule schema was dropped and recreated")
    }
  }
  def dumpUserSchema() = Action.async { implicit rs =>
    userRepo.dumpSchema().map(tup =>Ok (views.html.admin.show_schema(tup)))
  }

  def dropCreateUserSchema() = Action.async { implicit rs =>
    for (
      u1 <- userRepo.dropSchema();
      u2 <- userRepo.createSchema()
    ) yield {
      Redirect("/deepfij/admin").flashing("level"->"info", "message"->"User schema was dropped and recreated")
    }
  }


}
