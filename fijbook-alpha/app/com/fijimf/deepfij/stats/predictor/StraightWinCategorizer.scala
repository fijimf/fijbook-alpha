package com.fijimf.deepfij.stats.predictor

import com.fijimf.deepfij.models.{Game, Result}

case object StraightWinCategorizer extends Categorizer[(Game, Option[Result])] {
  override def numCategories: Int = 2

  override def categorize(t: (Game, Option[Result])): Option[Int] = t match {
    case (_, Some(rslt)) => Some(b2i(rslt.homeScore > rslt.awayScore))
    case (_, None) => None
  }

  def b2i(b: Boolean) = if (b) 0 else 1
}
