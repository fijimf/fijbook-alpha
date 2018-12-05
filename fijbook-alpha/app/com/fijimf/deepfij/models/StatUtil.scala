package com.fijimf.deepfij.models

import cats.implicits._
/**
  * Created by jimfrohnhofer on 6/29/17.
  */
object StatUtil {
  def transformSnapshot(svs: List[StatValue], map: StatValue => Team, higherIsBetter: Boolean): List[(Int, StatValue, Team)] = {
    svs.map(sv => map(sv) -> sv).sortBy(tup => if (higherIsBetter) -tup._2.value else tup._2.value).foldLeft(List.empty[(Int, StatValue, Team)])((accum: List[(Int, StatValue, Team)], response: (Team, StatValue)) => {
      val rank = accum match {
        case Nil => 1
        case head :: _ => if (head._2.value === response._2.value) head._1 else accum.size + 1
      }
      (rank, response._2, response._1) :: accum
    }).reverse
  }
}
