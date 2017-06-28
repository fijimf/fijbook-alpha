package com.fijimf.deepfij.stats

import java.time.LocalDate

import breeze.linalg.svd.DenseSVD
import breeze.linalg.{DenseMatrix, DenseVector, svd}
import breeze.stats._
import com.fijimf.deepfij.models.{Game, Result, Schedule, Team}
import play.api.Logger

case class LeastSquares(s: Schedule, dates:List[LocalDate]) extends Analyzer[LSAccumulator] {
  val log = Logger(LeastSquares.getClass)
  private val completedGames: List[(Game, Result)] = s.games.flatMap(g => s.resultMap.get(g.id).map(r => g -> r))
  lazy val data: Map[LocalDate, Map[Team, LSAccumulator]] = {
    dates.map(d => {
      log.info("Starting "+d)

      val results: (List[(Int, Game, Result)]) = completedGames.filter(_._1.date.isBefore(d)).zipWithIndex.map(t=>(t._2,t._1._1,t._1._2))
      if (results.isEmpty) {
        d -> Map.empty[Team, LSAccumulator]
      } else {
        val uniqueTeams = results.map(_._2.homeTeamId).toSet ++ results.map(_._2.awayTeamId).toSet
        val teamMap: Map[Long, Int] = uniqueTeams.zipWithIndex.toMap

        val nGames: Int = results.size
        val nTeams: Int = teamMap.size
        val A: DenseMatrix[Double] =new DenseMatrix(nGames, nTeams)
        val bTies: DenseVector[Double] =new DenseVector(nGames)
        val bNoTies: DenseVector[Double] =new DenseVector(nGames)
        val bScaledTies: DenseVector[Double] =new DenseVector(nGames)
        val bScaledNoTies: DenseVector[Double] =new DenseVector(nGames)
        val bLogPropTies: DenseVector[Double] =new DenseVector(nGames)
        val bLogPropNoTies: DenseVector[Double] =new DenseVector(nGames)
        results.foreach{case  (i: Int, g:Game, r:Result) =>
          A.update(i, teamMap(g.homeTeamId), 1.0)
          A.update(i, teamMap(g.awayTeamId), -1.0)
          bTies.update(i, tiesResult(r))
          bNoTies.update(i, noTiesResult(r))
          bScaledTies.update(i, scaledTiesResult(r))
          bScaledNoTies.update(i, scaledNoTiesResult(r))
          bLogPropTies.update(i, logPropTiesResult(r))
          bLogPropNoTies.update(i, logPropNoTiesResult(r))
        }

        normalize(bLogPropNoTies)
        normalize(bLogPropTies)

        val xs = solve(A,List (bTies, bNoTies, bScaledTies, bScaledNoTies, bLogPropTies, bLogPropTies))
        log.info("Completed "+d)
        d -> teamMap.map((tuple: (Long, Int)) => {
          val team = s.teamsMap(tuple._1)
          team -> LSAccumulator(xs(0)(tuple._2),xs(1)(tuple._2),xs(2)(tuple._2),xs(3)(tuple._2),xs(4)(tuple._2),xs(5)(tuple._2))
        })
      }
    }).toMap
  }

  private def normalize(vec: DenseVector[Double]): DenseVector[Double] = {
    val mav = meanAndVariance(vec)
    vec :-= mav.mean
    vec :/= mav.stdDev
  }

  private def tiesResult(r: Result): Double = {
    if (r.periods > 2) {
      0.0
    } else {
      r.homeScore - r.awayScore
    }
  }
  private def noTiesResult(r: Result): Double = r.homeScore - r.awayScore

  private def scaledTiesResult(r: Result): Double = math.signum(tiesResult(r))
  private def scaledNoTiesResult(r: Result): Double = math.signum(noTiesResult(r))

  private def logPropTiesResult(r: Result): Double = {
    if (r.periods > 2) {
      0.0
    } else {
      math.log(r.homeScore.toDouble/r.awayScore.toDouble)
    }
  }
  private def logPropNoTiesResult(r: Result): Double = math.log(r.homeScore.toDouble/r.awayScore.toDouble)



  def solve(A: DenseMatrix[Double], bs: List[DenseVector[Double]]): List[DenseVector[Double]] = {
    val svd1: DenseSVD = svd(A)
    val sigma = new DenseMatrix[Double](A.cols, A.rows)
    svd1.singularValues.toArray.zipWithIndex.foreach { case (ss: Double, i: Int) => if (ss > 0.1) sigma.update(i, i, 1.0 / ss) }

    bs.map(b=>svd1.rightVectors.t * sigma * svd1.leftVectors.t * b)
  }

  override val name: String = LeastSquares.name
  override val desc: String = LeastSquares.desc
  override val key: String = LeastSquares.key
  override val stats: List[Stat[LSAccumulator]] = LeastSquares.stats
}


case object LeastSquares extends Model[LSAccumulator] {

  def apply(s:Schedule):LeastSquares={
//    private val dates = completedGames.map(_._1.date.plusDays(1)).distinct.sortBy(_.toEpochDay)
    LeastSquares(s,s.resultDates)
  }
   val name: String = "Least Squares"
   val desc: String = "Simple regression model"
   val key: String = "least-squares"
   val stats: List[Stat[LSAccumulator]] = List(
    Stat[LSAccumulator]("X-margin[Ties]", "x-margin-ties", 0, higherIsBetter = true, _.xTies),
    Stat[LSAccumulator]("X-margin[No Ties]", "x-margin-no-ties", 0, higherIsBetter = true, _.xNoTies),
    Stat[LSAccumulator]("X-wins[Ties]", "x-wins-ties", 0, higherIsBetter = true, _.xScaledTies),
    Stat[LSAccumulator]("X-wins[No Ties]", "x-wins-no-ties", 0, higherIsBetter = true, _.xScaledNoTies),
    Stat[LSAccumulator]("X-log proportion[Ties]", "x-log-proportion-ties", 0, higherIsBetter = true, _.xLogPropTies),
    Stat[LSAccumulator]("X-log proportion[No Ties]", "x-log-proportion-no-ties", 0, higherIsBetter = true, _.xLogPropNoTies)
  )
}




