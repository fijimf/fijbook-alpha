package controllers

import java.time.LocalDate

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.ComputedStatisticService
import com.fijimf.deepfij.models.{StatUtil, StatValue, Team}
import com.fijimf.deepfij.stats.{Model, Stat}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json._
import play.api.mvc._
import controllers.silhouette.utils.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

class StatsController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 val teamDao: ScheduleDAO,
                                 val statWriterService: ComputedStatisticService,
                                 val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext)
  extends BaseController with I18nSupport {
  val log = Logger(this.getClass)

  import play.api.libs.json._

  def updateAll() = silhouette.SecuredAction.async { implicit rs =>
//    statWriterService.update()
    Future.successful(Redirect(routes.AdminController.index()).flashing("info" -> "Updating models for current schedule"))
  }

  def updateAllSeasons() = silhouette.SecuredAction.async { implicit rs =>
//    statWriterService.updateAllSchedules(None)
    Future.successful(Redirect(routes.AdminController.index()).flashing("info" -> "Updating models for current schedule"))
  }

  def updateAllSeasonsModel(k:String) = silhouette.SecuredAction.async { implicit rs =>
//    statWriterService.updateAllSchedules(Some(k))
    Future.successful(Redirect(routes.AdminController.index()).flashing("info" -> "Updating models for current schedule"))
  }

def todo = TODO
def todo1(m:String) = TODO
def todo2(M:String, k:String) = TODO



}


final case class ModelsContext(models: List[Model[_]]) {

}

final case class ModelContext(model: Model[_], stats: List[Stat[_]], xs: Map[LocalDate, Map[String, List[StatValue]]], teamMap: Map[Long, Team]) {
  def modelName = model.name

  def modelKey = model.key

  def latestDate = xs.keys.maxBy(_.toEpochDay)

  def latestValues(stat: Stat[_]): List[(Int, StatValue, Team)] = {
    xs(latestDate).get(stat.key) match {
      case Some(lsv) => StatUtil.transformSnapshot(lsv, (sv: StatValue) => teamMap(sv.teamID), stat.higherIsBetter)
      case None => List.empty[(Int, StatValue, Team)]
    }
  }

  def desc(stat: Stat[_]) = new DescriptiveStatistics(latestValues(stat).map(_._2.value).toArray)
}

final case class StatContext(model: Model[_], stat: Stat[_], xs: Map[LocalDate, List[StatValue]], teamMap: Map[Long, Team]) {

  def asMap2 = JsObject(Seq(
    "model" -> JsObject(Seq(
      "name" -> JsString(model.name),
      "key" -> JsString(model.key)
    )),
    "stat" -> JsObject(Seq(
      "name" -> JsString(stat.name),
      "key" -> JsString(stat.key),
      "higherIsBetter" -> JsBoolean(stat.higherIsBetter)
    )),
    "asOf" -> JsString(latestDate.toString),
    "descriptiveStats" -> JsObject(Seq(
      "mean" -> JsNumber(desc.getMean),
      "var" -> JsNumber(desc.getVariance),
      "min" -> JsNumber(desc.getMin),
      "q1" -> JsNumber(desc.getPercentile(25.0)),
      "med" -> JsNumber(desc.getPercentile(50.0)),
      "q3" -> JsNumber(desc.getPercentile(75.0)),
      "max" -> JsNumber(desc.getMax)
    )),
    "values" -> JsArray(latestValues.map { case (rk: Int, value: StatValue, team: Team) => JsObject(Seq("rank" -> JsNumber(rk), "value" -> JsNumber(value.value), "team" -> JsString(team.name), "teamKey" -> JsString(team.key))) }.toSeq)
  ))

  def asMap = JsArray(latestValues.map { case (rk: Int, value: StatValue, team: Team) => JsObject(Seq("rank" -> JsNumber(rk), "value" -> JsNumber(value.value), "team" -> JsString(team.name), "teamKey" -> JsString(team.key))) }.toSeq)

  def modelName = model.name

  def statName = stat.name

  def latestDate = xs.keys.maxBy(_.toEpochDay)

  def latestValues: List[(Int, StatValue, Team)] = {
    StatUtil.transformSnapshot(xs(latestDate), (sv: StatValue) => teamMap(sv.teamID), stat.higherIsBetter)
  }

  def desc = new DescriptiveStatistics(latestValues.map(_._2.value).toArray)

  def latestValuesAlpha = latestValues.sortBy(_._3.name)
}
