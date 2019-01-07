package com.fijimf.deepfij.models.nstats.predictors

import java.time.LocalDate

import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.{Game, Schedule, XStat}
import play.api.Logger

import scala.concurrent.Future

case class BaseLogisticFeatureExtractor(s: Schedule, ss: StatValueDAO) extends FeatureExtractor {
  val logger = Logger(this.getClass)
  import scala.concurrent.ExecutionContext.Implicits.global

  override def apply(gs: List[Game]): Future[List[Map[String, Double]]] = {
    val zero: (List[Future[Map[String, Double]]], CachedLoader) = (List.empty[Future[Map[String, Double]]], CachedLoader())
    val (list,_)=gs.foldLeft(zero) { case ((featureList, loader), g) =>
      val (features, loaderN) = loader.extract(g)
      (features :: featureList, loaderN)
    }
    Future.sequence(list.reverse)
  }

  case class CachedLoader(cache: Map[LocalDate, Future[Map[Long, XStat]]] = Map.empty[LocalDate, Future[Map[Long, XStat]]]) {

    def extract(g: Game): (Future[Map[String, Double]], CachedLoader) = {
      cache.get(g.date) match {
        case Some(fm) =>
          (fm.map(m =>
            createFeatureMap(g, m)), this)
        case None =>
          val h = loadSnapshot(g)
          (h.map(createFeatureMap(g, _)), copy(cache = cache + (g.date -> h)))
      }
    }

    def createFeatureMap(g: Game, map: Map[Long, XStat]): Map[String, Double] = {
      (for {
        hx <- map.get(g.homeTeamId)
        h <- hx.zScore
        ax <- map.get(g.awayTeamId)
        a <- ax.zScore
      } yield {
        Map(
          "ols-z-diff" -> (h - a)
        )
      }).getOrElse(Map.empty[String, Double])
    }

    private def loadSnapshot(g: Game): Future[Map[Long, XStat]] = {
      ss.findXStatsSnapshot(g.seasonId, g.date, modelKey = "ols", fallback = true).map(_.map(x => x.teamId -> x).toMap)
    }
  }
}
