package com.fijimf.deepfij.stats

import java.time.LocalDate

import com.fijimf.deepfij.models.Team

trait Model[S] {
  val name:String
  val desc:String
  val key:String
  val stats: List[Stat[S]]
}

trait Analyzer[S] extends Model[S] {

  val data: Map[LocalDate, Map[Team, S]]


  def value(k: String, t: Team, d: LocalDate): Option[Double] = {
    stats.find(_.key == k) match {
      case Some(s) =>
        data.get(d) match {
          case Some(m) =>
            m.get(t) match {
              case Some(u: S) => Some(s.f(u))
              case None => Some(s.defaultValue)
            }
          case None => None
        }
      case None => None
    }
  }
}
