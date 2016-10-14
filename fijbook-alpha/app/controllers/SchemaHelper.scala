package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.ScheduleRepository
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller, Result}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SchemaHelper @Inject()(repo: ScheduleRepository) extends Controller {

  def dumpSchema() = Action.async { implicit rs =>
    repo.dumpSchema().map(tup =>Ok (views.html.admin.show_schema(tup)))
  }

  def dropCreateSchema() = Action.async { implicit rs =>
    for (
      u1 <- repo.dropSchema();
      u2 <- repo.createSchema()
    ) yield {
      Redirect("/deepfij/admin").flashing("level"->"info", "message"->"Schema was dropped and recreated")
    }
  }


}
