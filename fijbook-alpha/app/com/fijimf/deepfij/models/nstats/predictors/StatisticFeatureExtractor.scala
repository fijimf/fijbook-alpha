package com.fijimf.deepfij.models.nstats.predictors

import java.time.LocalDate

import com.fijimf.deepfij.models.Game
import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import play.api.Logger

import scala.concurrent.Future

case class StatisticFeatureExtractor(statDao: StatValueDAO, stats: List[(String, String)]) extends FeatureExtractor {
  val logger = Logger(this.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  override def apply(games: List[Game]): Future[List[(Long,Map[String, Double])]] = {
    Future.sequence(games.groupBy(_.seasonId).flatMap { case (seasonId, gs) => featuresBySeason(seasonId, gs) }.toList)
  }

  def featuresBySeason(seasonId: Long, games: List[Game]): List[Future[(Long,Map[String, Double])]] = {
    games.groupBy(_.date).flatMap { case (date, gs) => featuresByDate(seasonId, date, gs) }.toList
  }

  def featuresByDate(seasonId: Long, date: LocalDate, games: List[Game]): List[Future[(Long, Map[String, Double])]] = {
    val fs = stats.map { case (stat, measure) => s"$stat.$measure" -> createLookup(seasonId, date, stat, measure) }
    games.map(g => {
      val futMap = Future.sequence(fs.map { case (key, futLookup) =>
        futLookup.map(lu => s"$key.home" -> lu.get(g.homeTeamId) :: s"$key.away" -> lu.get(g.awayTeamId) :: Nil)
      }).map(_.flatten.collect {
        case (k, Some(v)) => k -> v
      }.toMap)
      futMap.map(m=>g.id->m)
    })
  }

  def createLookup(seasonId: Long, date: LocalDate, stat: String, measure: String): Future[Map[Long, Double]] = {
    statDao.findXStatsSnapshot(seasonId, date, stat, fallback = true).map(_.map(xstat => {
      measure match {
        case "value" => xstat.teamId -> xstat.value
        case "rank" => xstat.teamId -> xstat.rank.map(_.toDouble)
        case "zscore" => xstat.teamId -> xstat.zScore
        case "percentile" => xstat.teamId -> xstat.percentile
        case _ => xstat.teamId -> xstat.value
      }
    }).collect {
      case (k, Some(v)) => k -> v
    }.toMap)
  }

}
