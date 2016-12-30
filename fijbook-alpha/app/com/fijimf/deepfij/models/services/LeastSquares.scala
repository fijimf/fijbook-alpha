package com.fijimf.deepfij.models.services

import java.time.LocalDate

import breeze.linalg.svd.DenseSVD
import breeze.linalg.{DenseMatrix, DenseVector, svd}
import com.fijimf.deepfij.models.{Game, Result, Schedule, Team}
import com.fijimf.deepfij.stats.{Analyzer, Stat}
import play.api.Logger

case class LeastSquares(s: Schedule) extends Analyzer[LSCalc] {


  val A=new DenseMatrix[Double](6,3)
  A.update(0,0,1.0)
  A.update(1,1,1.0)
  A.update(2,2,1.0)
  A.update(3,2,1.0)
  A.update(4,2,1.0)
  A.update(5,2,1.0)
  val b=new DenseVector(Array(1.0,2.0,5.0,2.0,3.0,6.0))
  val x = solve(A,b)
  println("************************************-->"+x.toString())


  override val name: String = "Least Squares"
  override val desc: String = "Simple regression model"
  override val key: String = "least-squares"

  val log = Logger(LeastSquares.getClass)
  private val completedGames: List[(Game, Result)] = s.games.flatMap(g => s.resultMap.get(g.id).map(r => g -> r))
  private val dates = completedGames.map(_._1.date.plusDays(1)).distinct.sortBy(_.toEpochDay)
  val data: Map[LocalDate, Map[Team, LSCalc]] = {
    dates.map(d => {
      log.info("Starting "+d)

      val results: (List[(Int, Game, Result)]) = completedGames.filter(_._1.date.isBefore(d)).zipWithIndex.map(t=>(t._2,t._1._1,t._1._2))
      if (results.isEmpty) {
        d -> Map.empty[Team, LSCalc]
      } else {
        val uniqueTeams = results.map(_._2.homeTeamId).toSet ++ results.map(_._2.awayTeamId).toSet
        val teamMap: Map[Long, Int] = uniqueTeams.zipWithIndex.toMap

        val nGames: Int = results.size
        val nTeams: Int = teamMap.size
        val A: DenseMatrix[Double] =new DenseMatrix(nGames, nTeams)
        val b: DenseVector[Double] =new DenseVector(nGames)
        results.foreach{case  (i: Int, g:Game, r:Result) =>
          A.update(i, teamMap(g.homeTeamId), 1.0)
          A.update(i, teamMap(g.awayTeamId), -1.0)
          b.update(i, if (r.periods > 2) {
              0.0
            } else {
              r.homeScore - r.awayScore
            }
          )
        }


        val x = solve(A,b)
        log.info("Completed "+d)
        d -> teamMap.map((tuple: (Long, Int)) => {
          val team = s.teamsMap(tuple._1)
          val value = x(tuple._2)
          if (team.name=="Villanova") log.info(team.name+"   "+value+"   "+tuple)
          team -> LSCalc(value)
        })
      }
    }).toMap
  }
  override val stats: List[Stat[LSCalc]] = List(
    Stat[LSCalc]("X", "x", 0, higherIsBetter = true, _.x))

  def solve(A: DenseMatrix[Double], b: DenseVector[Double]): DenseVector[Double] = {
    val svd1: DenseSVD = svd(A)
    val sigma = new DenseMatrix[Double](A.cols, A.rows)
    svd1.singularValues.toArray.zipWithIndex.foreach { case (ss: Double, i: Int) => if (ss > 0.01) sigma.update(i, i, 1.0 / ss) }

    svd1.rightVectors.t * sigma * svd1.leftVectors.t * b
  }


}

case class LSCalc(x: Double)


object FixIt {
  def main(args: Array[String]): Unit = {

  }
  def solve(A: DenseMatrix[Double], b: DenseVector[Double]): DenseVector[Double] = {
    val svd1: DenseSVD = svd(A)
    val sigma = new DenseMatrix[Double](A.cols, A.rows)
    svd1.singularValues.toArray.zipWithIndex.foreach { case (ss: Double, i: Int) => if (ss > 0.01) sigma.update(i, i, 1.0 / ss) }

    svd1.rightVectors * sigma * svd1.leftVectors * b
  }
}