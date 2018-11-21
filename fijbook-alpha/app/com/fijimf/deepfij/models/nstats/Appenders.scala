package com.fijimf.deepfij.models.nstats

import breeze.linalg.{DenseVector, max, min}
import com.fijimf.deepfij.models.{Game, Result}

object Appenders {

  trait base extends Analysis[Map[Long, List[Double]]] {

    override def zero: Map[Long, List[Double]] = Map.empty[Long, List[Double]]

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

  import breeze.stats.{mean, median, variance}


  trait margin extends base {
    override def appendKeyValues(g: Game, r: Result): List[(Long, Double)] = List(g.homeTeamId -> (r.homeScore - r.awayScore).toDouble, g.awayTeamId -> (r.awayScore - r.homeScore).toDouble)
  }

  trait combined extends base {
    override def appendKeyValues(g: Game, r: Result): List[(Long, Double)] = List(g.homeTeamId -> (r.homeScore + r.awayScore).toDouble, g.awayTeamId -> (r.awayScore + r.homeScore).toDouble)
  }

  trait pointsFor extends base {
    override def appendKeyValues(g: Game, r: Result): List[(Long, Double)] = List(g.homeTeamId -> r.homeScore.toDouble, g.awayTeamId -> r.awayScore.toDouble)
  }

  trait pointsAgainst extends base {
    override def appendKeyValues(g: Game, r: Result): List[(Long, Double)] = List(g.homeTeamId -> r.awayScore.toDouble, g.awayTeamId -> r.homeScore.toDouble)
  }

  object meanMargin extends margin {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => mean(z))

    override def key: String = "mean-margin"

    override def higherIsBetter: Boolean = true
  }

  object varianceMargin extends margin {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => variance(z))

    override def key: String = "variance-margin"

    override def higherIsBetter: Boolean = true

  }

  object maxMargin extends margin {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => max(z))

    override def key: String = "max-margin"

    override def higherIsBetter: Boolean = true
  }

  object minMargin extends margin {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => min(z))

    override def key: String = "min-margin"

    override def higherIsBetter: Boolean = true
  }

  object medianMargin extends margin {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => median(DenseVector(z.toArray)))

    override def key: String = "median-margin"

    override def higherIsBetter: Boolean = true
  }

  object meanCombined extends combined {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => mean(z))

    override def key: String = "mean-combined"

    override def higherIsBetter: Boolean = true
  }

  object varianceCombined extends combined {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => variance(z))

    override def key: String = "variance-combined"

    override def higherIsBetter: Boolean = true
  }

  object maxCombined extends combined {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => max(z))

    override def key: String = "max-combined"

    override def higherIsBetter: Boolean = true
  }

  object minCombined extends combined {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => min(z))

    override def key: String = "min-combined"

    override def higherIsBetter: Boolean = true
  }

  object medianCombined extends combined {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => median(DenseVector(z.toArray)))

    override def key: String = "median-combined"

    override def higherIsBetter: Boolean = true
  }


  object meanPointsFor extends margin {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => mean(z))

    override def key: String = "mean-points-for"

    override def higherIsBetter: Boolean = true
  }

  object variancePointsFor extends pointsFor {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => variance(z))

    override def key: String = "variance-points-for"

    override def higherIsBetter: Boolean = true
  }

  object maxPointsFor extends pointsFor {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => max(z))

    override def key: String = "max-points-for"

    override def higherIsBetter: Boolean = true
  }

  object minPointsFor extends pointsFor {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => min(z))

    override def key: String = "min-points-for"

    override def higherIsBetter: Boolean = true
  }

  object medianPointsFor extends pointsFor {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => median(DenseVector(z.toArray)))

    override def key: String = "median-points-for"

    override def higherIsBetter: Boolean = true
  }

  object meanPointsAgainst extends margin {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => mean(z))

    override def key: String = "mean-points-against"

    override def higherIsBetter: Boolean = true
  }

  object variancePointsAgainst extends pointsAgainst {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => variance(z))

    override def key: String = "variance-points-against"

    override def higherIsBetter: Boolean = true
  }

  object maxPointsAgainst extends pointsAgainst {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => max(z))

    override def key: String = "max-points-against"

    override def higherIsBetter: Boolean = false
  }

  object minPointsAgainst extends pointsAgainst {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => min(z))

    override def key: String = "min-points-against"

    override def higherIsBetter: Boolean = false
  }

  object medianPointsAgainst extends pointsAgainst {
    override def extract(b: Map[Long, List[Double]]): Map[Long, Double] = b.mapValues(z => median(DenseVector(z.toArray)))

    override def key: String = "median-points-against"

    override def higherIsBetter: Boolean = false
  }

}
