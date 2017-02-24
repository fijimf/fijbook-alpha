package com.fijimf.deepfij.stats.predictor

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{Game, Result}
import org.apache.mahout.math.{DenseVector, Vector}
import play.api.Logger

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class StatValueGameFeatureMapper(statKey: String, modelKey: String, dao: ScheduleDAO) extends FeatureMapper[(Game, Option[Result])] {
  val logger = Logger(this.getClass)
  import scala.concurrent.ExecutionContext.Implicits.global
  val values = Await.result(dao.loadStatValues(statKey, modelKey).map(sv => {
    logger.info(s"Loaded ${sv.size} model values")
    sv.groupBy(_.date).mapValues(_.map(s => s.teamID -> s).toMap)
  }), Duration.Inf)

  override def featureDimension: Int = 2

  override def featureName(i: Int): String = List("Intercept", statKey).apply(i)

  override def feature(t: (Game, Option[Result])): Option[Vector] = {
    val (g, _) = t
    val xs = values.keys.filter(_.isBefore(g.date))
    if (xs.nonEmpty) {
      val maxDate = xs.maxBy(_.toEpochDay)
      val x = values(maxDate)
      for {hx <- x.get(g.homeTeamId)
           ax <- x.get(g.awayTeamId)
      } yield new DenseVector(Array(1.0, hx.value - ax.value))
    } else {
      None
    }

  }

}
