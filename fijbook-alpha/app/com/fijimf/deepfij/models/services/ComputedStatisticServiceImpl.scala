package com.fijimf.deepfij.models.services

import akka.actor.ActorSystem
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.nstats._
import javax.inject.Inject
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class ComputedStatisticServiceImpl @Inject()(dao: ScheduleDAO, actorSystem: ActorSystem) extends ComputedStatisticService {

  val logger = Logger(this.getClass)

  private val wrapper = new StatsWrapper(dao, actorSystem)
  override val models: List[Analysis[_]] = List(
    Counters.games,
    Counters.wins,
    Counters.losses
//    Counters.homeWins,
//    Counters.homeLosses,
//    Counters.awayWins,
//    Counters.awayLosses,
//    Counters.otGames,
//    Counters.otWins,
//    Counters.otLosses,
//    Counters.winStreak,
//    Counters.lossStreak,
//    Appenders.meanMargin,
//    Appenders.varianceMargin,
//    Appenders.maxMargin,
//    Appenders.minMargin,
//    Appenders.medianMargin,
//    Appenders.meanCombined,
//    Appenders.varianceCombined,
//    Appenders.maxCombined,
//    Appenders.minCombined,
//    Appenders.medianCombined,
//    Appenders.meanPointsFor,
//    Appenders.variancePointsFor,
//    Appenders.maxPointsFor,
//    Appenders.minPointsFor,
//    Appenders.medianPointsFor,
//    Appenders.meanPointsAgainst,
//    Appenders.variancePointsAgainst,
//    Appenders.maxPointsAgainst,
//    Appenders.minPointsAgainst,
//    Appenders.medianPointsAgainst,
//    HigherOrderCounters.wins,
//    HigherOrderCounters.losses,
//    HigherOrderCounters.winPct,
//    HigherOrderCounters.oppWins,
//    HigherOrderCounters.oppLosses,
//    HigherOrderCounters.oppWinPct,
//    HigherOrderCounters.oppOppWins,
//    HigherOrderCounters.oppOppLosses,
//    HigherOrderCounters.oppOppWinPct,
//    HigherOrderCounters.rpi,
//    Regression.ols
  )

  override def update(year: Int, timeout: FiniteDuration): Future[String] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    dao.loadSchedule(year).flatMap {
      case Some(s) =>
        wrapper.updateStats(s, models, timeout).map(_.toString)
      case None =>
        Future("Schedule not found")
    }
  }
}

