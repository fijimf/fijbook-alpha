package com.fijimf.deepfij.stats

import java.time.LocalDate

import com.fijimf.deepfij.models.{Schedule, Team}
import play.api.Logger

trait Model[S] {
  val log = Logger(this.getClass)

  val name:String
  val desc:String
  val key:String
  val stats: List[Stat[S]]
  def create(s:Schedule, ds:List[LocalDate]):Option[Analyzer[S]]

  def canCreateDates(s:Schedule, ds:List[LocalDate]):List[LocalDate] = {
    if (s.games.isEmpty){
      log.warn("Attempting to create an analyzer on a schedule which has no games")
      List.empty[LocalDate]
    } else if (!s.gameResults.exists(_._2.isDefined)){
      log.warn("Attempting to create an analyzer on a schedule which has no results")
      List.empty[LocalDate]
    } else {
      val results = s.gameResults.filter(_._2.isDefined)
      val fd = ds.filter(!_.isBefore(results.head._1.date))
      if (fd.isEmpty){
        log.warn("Attempting to create an analyzer for dates prior to any results")
        List.empty[LocalDate]
      } else {
        fd
      }
    }
  }
}

trait Analyzer[S]  {
  val model:Model[S]
  val data: Map[LocalDate, Map[Team, S]]


  def value(k: String, t: Team, d: LocalDate): Option[Double] = {
    model.stats.find(_.key == k) match {
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
