package com.fijimf.deepfij.models.nstats

import com.fijimf.deepfij.models.{Game, Result, Schedule}


//TODO **** Single line per game with no constant coefficient is unstable, so
//  we will keep double line per game but also add
// TODO single line per game with constant coeff
// TODO single line per game with constant coeff, with ties
// TODO single line per game with constant coeff, scaled margin [12 points]
// TODO single line per game with constant coeff, just-win-baby
// TODO single line per game no constant coeff, lasso
// TODO single line per game no constant coeff, ridge

object Regression {

  final case class RegState(keyMap: Map[Long, Int] = Map.empty, A: Array[Array[Double]] = Array.empty[Array[Double]], b: Array[Double] = Array.empty[Double]) {

    require(A.forall(_.size == keyMap.size), s"Keymap knows ${keyMap.mkString(", ")} , ${A.zipWithIndex.find(_._1.size != keyMap.size).map(tup => s"${tup._2} --> [${tup._1.mkString(",")}]")}")
    require(A.size == b.size, s"A & b are different sizes, A rows ${A.size} b rows ${b.size}")

    def addTeam(id: Long): RegState = if (keyMap.contains(id)) {
      this
    } else {
      copy(keyMap = keyMap + (id -> keyMap.size), A = A.map(_ :+ 0.0))
    }

    def gameRowDiff(homeTeamId: Long, awayTeamId: Long): Array[Double] = Array.tabulate[Double](keyMap.size)(n => {
      if (n == keyMap.getOrElse(homeTeamId, -1)) {
        1.0
      } else if (n == keyMap.getOrElse(awayTeamId, -1)) {
        -1.0
      } else {
        0.0
      }
    })

    def gameRowSum(homeTeamId: Long, awayTeamId: Long): Array[Double] = Array.tabulate[Double](keyMap.size)(n => {
      if (n == keyMap.getOrElse(homeTeamId, -1)) {
        1.0
      } else if (n == keyMap.getOrElse(awayTeamId, -1)) {
        1.0
      } else {
        0.0
      }
    })

    def addGame(g: Game, r: Result): RegState = {
      val augmentedState = addTeam(g.homeTeamId).addTeam(g.awayTeamId)
      val a1 = augmentedState.gameRowDiff(g.homeTeamId, g.awayTeamId)
      val a2 = augmentedState.gameRowSum(g.homeTeamId, g.awayTeamId)
      val b1 = (r.homeScore - r.awayScore).toDouble
      val b2 = (r.homeScore + r.awayScore).toDouble
      augmentedState.copy(A = augmentedState.A :+ a1 :+ a2, b = augmentedState.b :+ b1 :+ b2)
      augmentedState.copy(A = augmentedState.A :+ a1, b = augmentedState.b :+ b1)
    }
  }

  object ols extends Analysis[RegState] {
    override def zero(s:Schedule): RegState = RegState()

    override def update(os: Option[Scoreboard], b: RegState): RegState = {
      os match {
        case Some(s) =>
          s.gs.foldLeft(b) { case (state, (game, result)) => state.addGame(game, result) }
        case None => b
      }
    }

    override def extract(state: RegState): Map[Long, Double] = {
      if (state.b.size <= state.keyMap.size) {
        Map.empty[Long, Double]
      } else {
        val ols = smile.regression.ols(state.A, state.b, "svd")
        val x = ols.coefficients

        state.keyMap.mapValues(x(_))
      }
    }

    override def key: String = "ols"

    override def higherIsBetter: Boolean = true

  }


}
