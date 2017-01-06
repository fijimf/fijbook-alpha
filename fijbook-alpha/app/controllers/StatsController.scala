package controllers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.{ScheduleDAO, StatValue, Team}
import com.fijimf.deepfij.models.services.StatisticWriterService
import com.fijimf.deepfij.stats.{Model, Stat}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Controller}
import utils.DefaultEnv

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class StatsController @Inject()(val teamDao: ScheduleDAO, val statWriterService: StatisticWriterService, val silhouette: Silhouette[DefaultEnv], val messagesApi: MessagesApi) extends Controller with I18nSupport {
val log=Logger(this.getClass)
  import scala.concurrent.ExecutionContext.Implicits.global
  def updateAll(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>

    teamDao.loadSchedules().map(ss => {
      val sortedSchedules = ss.sortBy(s => -s.season.year)
      sortedSchedules.headOption match {
        case Some(sch) => {
          statWriterService.updateForSchedule(sch).onComplete {
            case Success(x) =>
              log.info("************-->>" + x + "<<--*************")
            case Failure(thr) =>
              log.error("", thr)
              Redirect(routes.AdminController.index()).flashing("error" -> "Exception while updating models")
          }
          Redirect(routes.AdminController.index()).flashing("info" -> "Updating models for current schedule")
        }
        case None => Redirect(routes.IndexController.index()).flashing("info" -> "No current schedule loaded")
      }

    })

  }

  def updateAllByDate(yyyymmdd:String): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    statWriterService.update(LocalDate.parse(yyyymmdd, DateTimeFormatter.ofPattern("yyyyMMdd"))).foreach(_.onComplete {
      case Success(x) =>
        log.info("************-->>" + x + "<<--*************")
      case Failure(thr) =>
        log.error("", thr)
    })
    Future.successful(Redirect(routes.AdminController.index()).flashing("info" -> "Updating models for current schedule"))


  }

  def view(modelKey:String, statKey:String)=silhouette.UserAwareAction.async { implicit request =>
    statWriterService.lookupModel(modelKey).flatMap(m=>statWriterService.lookupStat(modelKey,statKey).map((m,_))) match {
      case Some((model,stat))=>{
        teamDao.loadSchedules().flatMap(ss => {
          val sortedSchedules = ss.sortBy(s => -s.season.year)
          sortedSchedules.headOption match {
            case Some(sch) => {
              teamDao.loadStatValues(statKey, modelKey).flatMap(stats => {
                val byDate = stats.groupBy(s=>s.date)
                val statContext = StatContext(model, stat, byDate,sch.teamsMap)
                Future.successful(Ok(views.html.data.stat(request.identity, statContext)))
              })
            }
            case None => Future.successful(Redirect(routes.IndexController.index()).flashing("info" -> "No current schedule loaded"))
          }
        })
      }
      case None=>  Future.successful(Redirect(routes.IndexController.index()).flashing("info" ->( "Could not identify model:stat '"+modelKey+":"+statKey+"'")))
    }
//            byDate.mapValues(lsv=>{
//              val stats = new DescriptiveStatistics(lsv.map(_.value).toArray)
//              val tv = lsv.map(sv=>sch.teamsMap(sv.teamID)->sv.value)

    }
}

case class StatContext(model:Model[_], stat:Stat[_], xs:Map[LocalDate,List[StatValue]], teamMap:Map[Long,Team]){
  def modelName=model.name
  def statName=stat.name
  def latestDate=xs.keys.maxBy(_.toEpochDay)
  def latestValues: List[(Int, StatValue, Team)] =xs(latestDate)
    .map(sv=>teamMap(sv.teamID)->sv).sortBy(tup=>if (stat.higherIsBetter) -tup._2.value else tup._2.value).foldLeft(List.empty[(Int, StatValue, Team)])((accum: List[(Int, StatValue, Team)], response: (Team, StatValue)) => {
    val rank = accum match {
      case Nil => 1
      case head::tail=> if (head._2.value==response._2.value) head._1 else accum.size+1
    }
    (rank, response._2, response._1)::accum
  }).reverse
  def desc=new DescriptiveStatistics(latestValues.map(_._2.value).toArray)
  def latestValuesAlpha=latestValues.sortBy(_._3.name)
}
