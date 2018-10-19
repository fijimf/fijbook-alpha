package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate
import java.time.temporal.ChronoUnit._

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{Game, Result}
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.spark.ml.linalg.{DenseVector, Vector}
import play.api.Logger

import scala.concurrent.Future


object StatValueGameFeatureMapper {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(this.getClass)

  val Z_SCORE = "zscore"

  val MIN_MAX = "minmax"

  val NO_NORMALIZATION = "no-normalization"

  def create(keys: List[String], normalizer: String, dao: ScheduleDAO): Future[StatValueGameFeatureMapper] = {

    require(Set(Z_SCORE, MIN_MAX, NO_NORMALIZATION).contains(normalizer), "Bad normalizer.  Allowed values '" + MIN_MAX + "', '" + Z_SCORE + "', '" + NO_NORMALIZATION + "'.")
    Future.sequence(keys.map(loadKey(_, normalizer, dao))).map(vs => StatValueGameFeatureMapper(keys, vs))
  }

  def loadKey(key: String, dao: ScheduleDAO): Future[Map[LocalDate, Map[Long, Double]]] = {
    key.split(":").toList match {
      case model :: stat :: Nil =>
        dao.loadStatValues(stat, model).map(_.groupBy(_.date).map { case (k, v) => k -> v.map(s => s.teamID -> s.value).toMap })
      case _ => Future.successful(Map.empty[LocalDate, Map[Long, Double]])
    }
  }

  def loadKey(key: String, norm: String, dao: ScheduleDAO): Future[Map[LocalDate, Map[Long, Double]]] = {
    loadKey(key, dao).map(_.map{ case(date,m) =>
      val (shift, scale) = normParms(norm, m.values)
      logger.info(s"K:$key D:$date => ($shift, $scale)")
      date->m.map { case (k, v) => k -> (v - shift) / scale }
    })
  }

  def normParms(n: String, xs: Iterable[Double]): (Double, Double) = {
    n.toLowerCase.trim match {
      case s: String if s == Z_SCORE =>
        val ds = new DescriptiveStatistics(xs.toArray)
        if (ds.getStandardDeviation == 0.0){
          (ds.getMean, 1.0)
        } else {
          (ds.getMean, ds.getStandardDeviation)
        }
      case s: String if s == MIN_MAX =>
        val ds = new DescriptiveStatistics(xs.toArray)
        if ((ds.getMax - ds.getMin)== 0.0){
          (ds.getMin, 1.0)
        } else {
          (ds.getMin, ds.getMax - ds.getMin)
        }

      case _ => (0.0, 1.0)
    }
  }
}

final case class StatValueGameFeatureMapper(keys: List[String], vals: List[Map[LocalDate, Map[Long, Double]]]) extends FeatureMapper[(Game, Option[Result])] {

  val logger = Logger(this.getClass)
  require(keys.lengthCompare(vals.size) == 0, "Value map and key list have different dimension")
  logger.info("StatValueGameMapper created with")
  vals.zip(keys).foreach { case (valueMap: Map[LocalDate, Map[Long, Double]], k: String) => {
    logger.info(s"$k has ${valueMap.size} dates")
    logger.info(s"$k first date is ${valueMap.keys.minBy(_.toEpochDay)}")
    logger.info(s"$k last date is ${valueMap.keys.maxBy(_.toEpochDay)}")
  }
  }
  val dateKey: List[Map[LocalDate, LocalDate]] = vals.map(vm => {
    val minDate = vm.keys.minBy(_.toEpochDay)
    val maxDate = vm.keys.maxBy(_.toEpochDay)
    1.to(DAYS.between(minDate.plusDays(1), maxDate.plusDays(180)).toInt).foldLeft(List.empty[(LocalDate, LocalDate)])((d2d: List[(LocalDate, LocalDate)], i: Int) => {
      val d = minDate.plusDays(i)
      val d0 = minDate.plusDays(i - 1)
      if (vm.contains(d0))
        (d -> d0) :: d2d
      else
        (d -> d2d.head._2) :: d2d
    }).toMap
  })
  dateKey.zip(keys).foreach { case (dateMap: Map[LocalDate, LocalDate], k: String) => {
    logger.info(s"$k date map has ${dateMap.size} dates")
    logger.info(s"$k first date is ${dateMap.keys.minBy(_.toEpochDay)}")
    logger.info(s"$k last date is ${dateMap.keys.maxBy(_.toEpochDay)}")
  }
  }


  override def featureDimension: Int = vals.size + 1

  override def featureName(i: Int): String = i match {
    case 0 => "Intercept"
    case n => keys(n - 1)
  }

  override def feature(t: (Game, Option[Result])): Option[Vector] = {
    val (g, _) = t
    val list = vals.zipWithIndex.map { case (m, i) => {
      dateKey(i).get(g.date) match {
        case Some(dt) =>
          vals(i).get(dt) match {
            case Some(n) =>
              (n.get(g.homeTeamId), n.get(g.awayTeamId)) match {
                case (Some(h), Some(a)) => Some(h - a)
                case _ => None
              }
            case None => None //throw new IllegalStateException("Key provided by map missing")
          }
        case None =>
          val last = dateKey(i).keySet.maxBy(_.toEpochDay)
          if (g.date.isAfter(last)) {
            vals(i).get(last) match {
              case Some(n) =>
                (n.get(g.homeTeamId), n.get(g.awayTeamId)) match {
                  case (Some(h), Some(a)) => Some(h - a)
                  case _ => None
                }
              case None => None //throw new IllegalStateException("Key provided by map missing")
            }
          } else {
            None
          }
      }
    }
    }
    list.foldLeft(Option(List(1.0)))((fs: Option[List[Double]], of: Option[Double]) => {
      of match {
        case Some(f) => fs.map(l => f :: l)
        case None => None
      }
    }).map(l => new DenseVector(l.toArray))
  }
}
