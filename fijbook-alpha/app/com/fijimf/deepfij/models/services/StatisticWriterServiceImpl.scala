package com.fijimf.deepfij.models.services

import javax.inject.Inject

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.stats.{Analyzer, Rpi, Scoring, WonLost}
import play.api.Logger

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.language.postfixOps

class StatisticWriterServiceImpl @Inject()(dao: ScheduleDAO) extends StatisticWriterService {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(this.getClass)

  val activeYear = 2017


  def update(): Option[Future[Option[Int]]] ={
    val result: Option[Schedule] = Await.result(dao.loadSchedules().map(_.find(_.season.year == activeYear)), Duration.Inf)
    result.map(sch => {
      updateForSchedule(sch)
    })
  }

  def updateForSchedule(sch: Schedule): Future[Option[Int]] = {
//    val models: List[Analyzer[_]] = List(WonLost(sch), Scoring(sch), Rpi(sch), LeastSquares(sch))
    val models: List[Analyzer[_]] = List( LeastSquares(sch))
    val dates = sch.lastResult match {
      case Some(d) => sch.season.dates.filter(sd => sd.isBefore(d))
      case None => List.empty
    }

    Await.result(dao.deleteStatValues(dates, models.map(_.key)), 5.minutes)

    val values: List[StatValue] = (for (m <- models;
                                        s <- m.stats;
                                        d <- dates;
                                        t <- sch.teams
    ) yield {
      m.value(s.key, t, d).map(x =>
        if (x.isInfinity){
          logger.warn("For "+d+", "+s.key+", "+t.name+" value is Infinity.  Setting to default value "+s.defaultValue)
          StatValue(0L, m.key, s.key, t.id, d, s.defaultValue)
        }else if (x.isNaN) {
          logger.warn("For "+d+", "+s.key+", "+t.name+" value is NaN.  Setting to default value "+s.defaultValue)
          StatValue(0L, m.key, s.key, t.id, d, s.defaultValue)
        } else {
          StatValue(0L, m.key, s.key, t.id, d, x)
        }
      )
    }).flatten
    dao.saveStatValues(values)
  }
}