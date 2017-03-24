package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{Game, Result, StatValue}
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.mahout.math.{DenseVector, Vector}
import play.api.Logger

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

sealed trait FeatureNormalization

case object FeatureNormalizationZScoreByDate

case object FeatureNormalizationZScoreByPop

case object FeatureNormalizationMinMaxByDate

case object FeatureNormalizationMinMaxPop

case object FeatureNormalizationNone


case class StatValueGameFeatureMapper(date: LocalDate, keys: List[String], dao: ScheduleDAO) extends FeatureMapper[(Game, Option[Result])] {
  val logger = Logger(this.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  val vals: Map[String, Map[Long, StatValue]] = Await.result(Future.sequence(keys.map(k => {
    (k.split(":").toList match {
      case model :: stat :: rest =>
        dao.loadStatValues(stat, model).map(sv => {
          logger.info(s"Loaded ${sv.size} model values")
          val valuesMap = sv.filter(_.date.isBefore(date)).groupBy(_.date).mapValues(_.map(s => s.teamID -> s).toMap)
          if (valuesMap.isEmpty) {
            Map.empty[Long, StatValue]
          } else {
            valuesMap(valuesMap.keySet.maxBy(_.toEpochDay))
          }
        })
      case _ => Future.successful(Map.empty[Long, StatValue])
    }).map(k -> _)
  })).map(_.toMap), Duration.Inf)

  val norms: Map[String, (Double, Double)] = keys.map(k => {
    k -> (k.split(":").toList match {
      case _ :: _ :: norm :: rest =>
        val ds = new DescriptiveStatistics(vals(k).values.map(_.value).toArray)
        norm match {
          case "z" => (ds.getMean, ds.getStandardDeviation)
          case "minmax" => (ds.getMin, ds.getMax - ds.getMin)
          case _ => (0.0, 1.0)
        }

      case _ :: _ :: Nil => (0.0, 1.0)
    })
  }).toMap

  override def featureDimension: Int = vals.size + 1

  override def featureName(i: Int): String = i match {
    case 0 => "Intercept"
    case n => keys(n - 1)
  }

  override def feature(t: (Game, Option[Result])): Option[Vector] = {
    val (g, _) = t
    val fs: List[Option[Double]] = keys.map(k => {
      for {xs <- vals.get(k)
           (shift, scale) <- norms.get(k)
           hx <- xs.get(g.homeTeamId).map(_.value)
           ax <- xs.get(g.awayTeamId).map(_.value)
      } yield {
        (hx - shift) / scale - (ax - shift) / scale
      }
    })

    if (fs.forall(_.isDefined)) {
      Some(new DenseVector((1.0 :: fs.map(_.get)).toArray))
    } else {
      None
    }
  }



}
