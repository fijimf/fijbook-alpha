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

  private val wrapper = new StatsUpdater(dao, actorSystem)

  override def update(year: Int, timeout: FiniteDuration): Future[String] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    dao.loadSchedule(year).flatMap {
      case Some(s) =>
        wrapper.updateStats(s, models, timeout).map(_.toString)
      case None =>
        Future("Schedule not found")
    }
  }

  override val models: List[Analysis[_]] = Analysis.models.map(_._2)
}

