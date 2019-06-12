package com.fijimf.deepfij.models.nstats

import breeze.linalg.{DenseVector, max, min}
import breeze.stats.{mean, median, variance}
import com.fijimf.deepfij.models.nstats.Appender.ResultValue
import com.fijimf.deepfij.models.{Game, Result, Schedule}

object Appender {

  type ResultValue = (Game, Result) => Double
  trait base extends Analysis[Map[Long, List[Double]]] {

    override def zero(s:Schedule): Map[Long, List[Double]] = Map.empty[Long, List[Double]]

    override def update(os: Option[Scoreboard], b: Map[Long, List[Double]]): Map[Long, List[Double]] = os match {
      case Some(sb) =>
        sb.gs.foldLeft(b) {
          case (map, (game, result)) =>
            appendKeyValues(game, result).foldLeft(map) {
              case (m1, (k, v)) => m1 + (k -> (v :: m1.getOrElse(k, List.empty[Double])))
            }
        }

      case None => b
    }

    def appendKeyValues(g: Game, r: Result): List[(Long, Double)]

    override def extract(b: Map[Long, List[Double]]): Map[Long, Double]
  }


  private val listMean: List[Double] => Option[Double] = {
    case Nil => None
    case l => Some(mean(l))
  }

  private val listVariance: List[Double] => Option[Double] = {
    case Nil => None
    case l => Some(variance(l))
  }

  private val listMax: List[Double] => Option[Double] = {
    case Nil => None
    case l => Some(max(l))
  }

  private val listMin: List[Double] => Option[Double] = {
    case Nil => None
    case l => Some(min(l))
  }

  private val listMedian: List[Double] => Option[Double] = {
    case Nil => None
    case l => Some(median(DenseVector(l.toArray)))
  }

  val meanMargin = Appender(
    key = "mean-margin",
    homeValue = (_, r) => r.homeScore - r.awayScore,
    awayValue = (_, r) => r.awayScore - r.homeScore,
    aggregator = listMean
  )

  val varianceMargin: Appender = meanMargin.copy(key = "variance-margin", aggregator = listVariance)
  val maxMargin: Appender = meanMargin.copy(key = "max-margin", aggregator = listMax)
  val minMargin: Appender = meanMargin.copy(key = "min-margin", aggregator = listMin)
  val medianMargin: Appender = meanMargin.copy(key = "median-margin", aggregator = listMedian)

  val meanCombined = Appender(
    key = "mean-combined",
    homeValue = (_, r) => r.homeScore + r.awayScore,
    awayValue = (_, r) => r.awayScore + r.homeScore,
    aggregator = listMean
  )

  val varianceCombined: Appender = meanCombined.copy(key = "variance-combined", aggregator = listVariance)
  val maxCombined: Appender = meanCombined.copy(key = "max-combined", aggregator = listMax)
  val minCombined: Appender = meanCombined.copy(key = "min-combined", aggregator = listMin)
  val medianCombined: Appender = meanCombined.copy(key = "median-combined", aggregator = listMedian)

  val meanPointsFor = Appender(
    key = "mean-points-for",
    homeValue = (_, r) => r.homeScore,
    awayValue = (_, r) => r.awayScore,
    aggregator = listMean
  )

  val variancePointsFor: Appender = meanPointsFor.copy(key = "variance-points-for", aggregator = listVariance)
  val maxPointsFor: Appender = meanPointsFor.copy(key = "max-points-for", aggregator = listMax)
  val minPointsFor: Appender = meanPointsFor.copy(key = "min-points-for", aggregator = listMin)
  val medianPointsFor: Appender = meanPointsFor.copy(key = "median-points-for", aggregator = listMedian)

  val meanPointsAgainst = Appender(
    key = "mean-points-against",
    homeValue = (_, r) => r.awayScore,
    awayValue = (_, r) => r.homeScore,
    aggregator = listMean
  )
  val variancePointsAgainst: Appender = meanPointsAgainst.copy(key = "variance-points-against", aggregator = listVariance)
  val maxPointsAgainst: Appender = meanPointsAgainst.copy(key = "max-points-against", aggregator = listMax)
  val minPointsAgainst: Appender = meanPointsAgainst.copy(key = "min-points-against", aggregator = listMin)
  val medianPointsAgainst: Appender = meanPointsAgainst.copy(key = "median-points-against", aggregator = listMedian)

}


case class Appender
(
  key: String,
  higherIsBetter: Boolean = true,
  homeValue: ResultValue = (_, _) => 0,
  awayValue: ResultValue = (_, _) => 0,
  aggregator: List[Double] => Option[Double]
) extends Analysis[Map[Long, List[Double]]] {

  override def zero(s: Schedule): Map[Long, List[Double]] = s.teams.map(_.id -> List.empty[Double]).toMap

  override def update(os: Option[Scoreboard], b: Map[Long, List[Double]]): Map[Long, List[Double]] = os match {
    case Some(sb) =>
      sb.gs.foldLeft(b) {
        case (map, (game, result)) =>
          keyValues(game, result).foldLeft(map) {
            case (m1, (k, v)) => m1 + (k -> (v :: m1.getOrElse(k, List.empty[Double])))
          }
      }
    case None => b
  }

  override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = {
    b.mapValues(aggregator).collect { case (k, Some(v)) => k -> v }
  }

  override val bounds: (Double, Double) = (Double.NegativeInfinity, Double.PositiveInfinity)

  private def keyValues(g: Game, r: Result): List[(Long, Double)] =
    List(g.homeTeamId -> homeValue(g, r), g.awayTeamId -> awayValue(g, r))
}

