package com.fijimf.deepfij.schedule.model

import scala.math.Ordering
import cats.implicits._

final case class ConferenceStandings(records: List[(WonLostRecord, WonLostRecord, Team)])

object ConferenceStandings {

  implicit def orderingRecords(implicit ord1: Ordering[WonLostRecord]): Ordering[(WonLostRecord, WonLostRecord, Team)] = new Ordering[(WonLostRecord, WonLostRecord, Team)] {
    def compare(x: (WonLostRecord, WonLostRecord, Team), y: (WonLostRecord, WonLostRecord, Team)): Int = {
      val compare1 = ord1.compare(x._1, y._1)
      if (compare1 != 0) return compare1
      val compare2 = ord1.compare(x._2, y._2)
      if (compare2 != 0) return compare2
      val compare3 = x._3.name.compare(y._3.name)
      if (compare3 != 0) return compare3
      0
    }
  }

  def apply(s: Schedule): List[(Conference, ConferenceStandings)] = {
    import s._
    s.conferences.map(c => {
      s.conferenceTeams.get(c.id) match {
        case Some(teamIds) =>
          val maybeTuples: List[(WonLostRecord, WonLostRecord, Team)] = teamIds.flatMap(tid => {
            s.teamsMap.get(tid).map(team => {
              val overallGames = s.completeGames.filter(_.hasTeam(tid))
              val confGames = overallGames.filter(_.isConferenceGame)
              val regConfGames = confGames.filter(_._1.tourneyKey.isEmpty)
              (
                WonLostRecord(
                  regConfGames.count(_.isWinner(team)),
                  regConfGames.count(_.isLoser(team))
                ),
                WonLostRecord(
                  overallGames.count(_.isWinner(team)),
                  overallGames.count(_.isLoser(team))
                ),
                team
              )
            })
          })
          (c, ConferenceStandings(maybeTuples.sortWith { case ((xc, xn, xt), (yc, yn, yt)) =>
            val confCmp = WonLostRecord.compare(xc, yc)
            if (confCmp === 0) {
              val ncCmp = WonLostRecord.compare(xn, yn)
              if (ncCmp === 0) {
                xt.name > yt.name
              } else {
                ncCmp > 0
              }
            } else {
              confCmp > 0
            }
          }))
        case None => (c, ConferenceStandings(List.empty[(WonLostRecord, WonLostRecord, Team)]))
      }
    }
    ).sortBy(_._1.name)
  }
}