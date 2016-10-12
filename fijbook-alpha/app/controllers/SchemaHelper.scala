package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.ScheduleRepository
import models.{ProjectRepo, TaskRepo}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller, Result, Results}

import scala.concurrent.{ExecutionContext, Future}

class SchemaHelper @Inject()(repo: ScheduleRepository) extends Controller {

  def dumpSchema() = Action.async { implicit rs =>
    repo.dumpSchema().map(tup =>Ok (views.html.admin.show_schema(tup)))
  }
  def createSchema() = Action.async { implicit rs =>
    repo.createSchema().map(unit => Ok("Schema created"))
  }
  def dropSchema() = Action.async { implicit rs =>
    repo.dropSchema().map(unit => Ok("Schema dropped"))
  }

}
