package controllers

import java.time.LocalDate

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.GamePredictorService
import com.fijimf.deepfij.stats._
import com.fijimf.deepfij.stats.predictor.{LogisticRegressionTrainer, StatValueGameFeatureMapper, StraightWinCategorizer}
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
  val features: List[(String,String)] = List(WonLost, Scoring, Rpi, LeastSquares).flatMap(m=>{
    m.stats.map(s=> s"${m.key}:${s.key}" -> s"${m.name}::${s.name}")
  })
  val normalizations = List("none"->"None","minmax"->"Min-Max","zscore"->"Z-Score")

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
          BadRequest(views.html.admin.testModel(rs.identity,ls,ts,features, normalizations, form))
        }
      },
      data => {
        StatValueGameFeatureMapper.create(data.features,data.normalization, teamDao).flatMap{fm=>
          val regTrainer: Future[LogisticRegressionTrainer] = LogisticRegressionTrainer.create(fm, StraightWinCategorizer, data.seasonsIncluded.map(_.toInt), teamDao)
          regTrainer.map(_.classifier).map(classifier=>
            teamDao.loadLatestSchedule().map(_.foreach(sch=>{
              sch.gameResults.filter(_._1.date==LocalDate.now()).map(gor=>gor->classifier.classify(gor)).foreach(println(_))
            }))
          )
          regTrainer.map(trainer=>
          Redirect(routes.AdminController.index()).flashing("info" -> s"${trainer.classifier.betaCoefficients.mkString(";")}")
          )
        }

      }
    )
  }

}
