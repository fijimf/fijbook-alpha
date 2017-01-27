package controllers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.services.StatisticWriterService
import com.fijimf.deepfij.models.{ScheduleDAO, StatUtil, StatValue, Team}
import com.fijimf.deepfij.stats.{Model, Stat}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller}
import utils.DefaultEnv

import scala.concurrent.Future
import scala.util.{Failure, Success}

class StatsController @Inject()(val teamDao: ScheduleDAO, val statWriterService: StatisticWriterService, val silhouette: Silhouette[DefaultEnv], val messagesApi: MessagesApi) extends Controller with I18nSupport {
  val log = Logger(this.getClass)

  import play.api.libs.json._

  import scala.concurrent.ExecutionContext.Implicits.global

  def updateAll(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>

    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      sortedSchedules.headOption match {
        case Some(sch) => {
          statWriterService.update()
          Redirect(routes.AdminController.index()).flashing("info" -> "Updating models for current schedule")
        }
        case None => Redirect(routes.IndexController.index()).flashing("info" -> "No current schedule loaded")
      }

    })

  }


  def viewStat(modelKey: String, statKey: String) = silhouette.UserAwareAction.async { implicit request =>
    statWriterService.lookupModel(modelKey).flatMap(m => statWriterService.lookupStat(modelKey, statKey).map((m, _))) match {
      case Some((model, stat)) => {
        teamDao.loadSchedules().flatMap(ss => {
          val sortedSchedules = ss.sortBy(s => -s.season.year)
          sortedSchedules.headOption match {
            case Some(sch) => {
              teamDao.loadStatValues(statKey, modelKey).flatMap(stats => {
                val byDate = stats.groupBy(s => s.date)
                val statContext = StatContext(model, stat, byDate, sch.teamsMap)
                Future.successful(Ok(views.html.data.stat(request.identity, statContext)))
              })
            }
            case None => Future.successful(Redirect(routes.IndexController.index()).flashing("info" -> "No current schedule loaded"))
          }
        })
      }
      case None => Future.successful(Redirect(routes.IndexController.index()).flashing("info" -> ("Could not identify statmodel:stat '" + modelKey + ":" + statKey + "'")))
    }
  }

  def viewStatApi(modelKey: String, statKey: String) = silhouette.UserAwareAction.async { implicit request =>
    statWriterService.lookupModel(modelKey).flatMap(m => statWriterService.lookupStat(modelKey, statKey).map((m, _))) match {
      case Some((model, stat)) => {
        teamDao.loadSchedules().flatMap(ss => {
          val sortedSchedules = ss.sortBy(s => -s.season.year)
          sortedSchedules.headOption match {
            case Some(sch) => {
              teamDao.loadStatValues(statKey, modelKey).map(stats => {
                val byDate = stats.groupBy(s => s.date)
                val statContext = StatContext(model,stat,byDate, sch.teamsMap)
                Ok(Json.toJson(statContext.asMap))
              })
            }
            case None => Future.successful(Ok(Json.toJson(Map.empty[String, String])))
          }
        })
      }
      case None => Future.successful(Ok(Json.toJson(Map.empty[String, String])))
    }
  }

  def viewModel(modelKey: String) = silhouette.UserAwareAction.async { implicit request =>
    statWriterService.lookupModel(modelKey) match {
      case Some(model) => {
        teamDao.loadSchedules().flatMap(ss => {
          val sortedSchedules = ss.sortBy(s => -s.season.year)
          sortedSchedules.headOption match {
            case Some(sch) => {
              teamDao.loadStatValues(modelKey).flatMap(stats => {
                val byDate = stats.groupBy(s => s.date).mapValues(_.groupBy(_.statKey))

                val statContext = ModelContext(model, model.stats, byDate, sch.teamsMap)
                Future.successful(Ok(views.html.data.statmodel(request.identity, statContext)))
              })
            }
            case None => Future.successful(Redirect(routes.IndexController.index()).flashing("info" -> "No current schedule loaded"))
          }
        })
      }
      case None => Future.successful(Redirect(routes.IndexController.index()).flashing("info" -> ("Could not identify model '" + modelKey + "'")))
    }
  }

  def viewModels() = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(Ok(views.html.data.statmodels(request.identity, ModelsContext(statWriterService.models))))
  }
}


case class ModelsContext(models: List[Model[_]]) {

}

case class ModelContext(model: Model[_], stats: List[Stat[_]], xs: Map[LocalDate, Map[String, List[StatValue]]], teamMap: Map[Long, Team]) {
  def modelName = model.name

  def modelKey = model.key

  def latestDate = xs.keys.maxBy(_.toEpochDay)

  def latestValues(stat: Stat[_]): List[(Int, StatValue, Team)] = {
    xs(latestDate).get(stat.key) match {
      case Some(lsv) => StatUtil.transformSnapshot(lsv, (sv: StatValue) => teamMap(sv.teamID), stat.higherIsBetter)
      case None => List.empty[(Int, StatValue, Team)]
    }
  }

  def desc(stat:Stat[_]) = new DescriptiveStatistics(latestValues(stat).map(_._2.value).toArray)
}

case class StatContext(model: Model[_], stat: Stat[_], xs: Map[LocalDate, List[StatValue]], teamMap: Map[Long, Team]) {

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
