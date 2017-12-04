package controllers

import com.fijimf.deepfij.models.Team
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.GamePredictorService
import com.fijimf.deepfij.stats._
import com.fijimf.deepfij.stats.predictor.{LogisticRegressionContext, StatValueGameFeatureMapper, StraightWinCategorizer}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import forms.PredictionModelForm
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import utils.DefaultEnv

import scala.concurrent.Future

class PredictionModelController @Inject()(
                                      val controllerComponents: ControllerComponents,
                                      val teamDao: ScheduleDAO,
                                      val gamePredictorService: GamePredictorService,
                                      val silhouette: Silhouette[DefaultEnv]
                                    )
  extends BaseController with I18nSupport {
  val log = Logger(this.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  val features: List[(String, String)] = List(WonLost, Scoring, Rpi, LeastSquares).flatMap(m => {
    m.stats.map(s => s"${m.key}:${s.key}" -> s"${m.name}::${s.name}")
  })
  val normalizations = List("none" -> "None", "minmax" -> "Min-Max", "zscore" -> "Z-Score")

  def test() = silhouette.SecuredAction.async { implicit rs =>
    for {
      ls <- teamDao.listSeasons
      ts <- teamDao.listTeams
    } yield {
      Ok(views.html.admin.testModel(rs.identity, ls, ts, features, normalizations, PredictionModelForm.form))
    }
  }

  def calibrate() = silhouette.SecuredAction.async { implicit rs =>
    PredictionModelForm.form.bindFromRequest.fold(
      form => {
        for {
          ls <- teamDao.listSeasons
          ts <- teamDao.listTeams
        } yield {
          log.warn(s"Bad request $form")
          BadRequest(views.html.admin.testModel(rs.identity, ls, ts, features, normalizations, form))
        }
      },
      data => {
        StatValueGameFeatureMapper.create(data.features, data.normalization, teamDao).flatMap { fm =>
          LogisticRegressionContext.selectTrainingSet(
            data.seasonsIncluded.map(_.toInt),
            data.excludeMonths.map(_.toInt),
            teamDao
          ).flatMap(gors => {
            teamDao.listTeams.flatMap(teamList => {
              val context: LogisticRegressionContext = LogisticRegressionContext.create(fm, StraightWinCategorizer, gors, teamDao)
              val resultLines = context.modelPerformance(gors)
              val seasMonthSplits: Map[(Long, Int), (Int, Int, Int, Double)] = context.performanceSplits(resultLines, (lrl) => (lrl.game.seasonId, lrl.game.date.getMonthValue))
              val seasHomeSplits = context.performanceSplits(resultLines, (lrl) => (lrl.game.seasonId, lrl.game.homeTeamId))
              val seasAwaySplits = context.performanceSplits(resultLines, (lrl) => (lrl.game.seasonId, lrl.game.awayTeamId))
              val teamSplitKeys = seasHomeSplits.keySet++seasAwaySplits.keySet
              val seasTeamSplits = teamSplitKeys.map(k=>{
                (seasHomeSplits.get(k), seasAwaySplits.get(k)) match {
                  case (Some(h),Some(a))=>
                    k -> (h._1 + a._1, h._2 + a._2, h._3 + a._3, (h._1 * h._4) + (a._1 * a._4) / (a._1 + h._1))
                  case (None,Some(a))=> k->a
                  case (Some(h),None)=> k->h
                  case _=>throw new IllegalArgumentException("Couldn't find"+k.toString())
                }
              }).toList

              Future.sequence(seasMonthSplits.keySet.map(t => teamDao.findSeasonById(t._1))).map(_.toList.flatten).map(seasons => {
                val months: List[Int] = seasMonthSplits.keySet.map(_._2).min.to(seasMonthSplits.keySet.map(_._2).max).toList
                val teamMap: Map[Long, Team] = teamList.map(t => t.id -> t).toMap

                val worst5BySeason: List[((Long, Long), (Int, Int, Int, Double))] = seasons.flatMap(s => seasTeamSplits.filter(_._1._1 == s.id).sortBy(_._2._4).takeRight(5))
                Ok(views.html.admin.logreg(
                  rs.identity,
                  resultLines,
                  seasMonthSplits,
                  worst5BySeason,
                  seasons,
                  teamMap)
                )
              })
            })
          })
        }
      }
    )
  }
}
