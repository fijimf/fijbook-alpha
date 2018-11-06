package controllers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.GamePredictorService
import com.fijimf.deepfij.models.{Result => _, _}
import com.fijimf.deepfij.stats._
import com.fijimf.deepfij.stats.predictor._
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
import forms.PredictionModelForm
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import controllers.silhouette.utils.DefaultEnv

import scala.concurrent.Future

class PredictionModelController @Inject()(
                                           val controllerComponents: ControllerComponents,
                                           val dao: ScheduleDAO,
                                           val gamePredictorService: GamePredictorService,
                                           val silhouette: Silhouette[DefaultEnv]
                                         )
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {
  val log = Logger(this.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  val features: List[(String, String)] = List(WonLost, Scoring, Rpi, LeastSquares).flatMap(m => {
    m.stats.map(s => s"${m.key}:${s.key}" -> s"${m.name}::${s.name}")
  })
  val normalizations = List(StatValueGameFeatureMapper.NO_NORMALIZATION -> "None",StatValueGameFeatureMapper.MIN_MAX -> "Min-Max", StatValueGameFeatureMapper.Z_SCORE -> "Z-Score")

  def logisticRequest() = silhouette.UserAwareAction.async { implicit rs =>
    for {
      ls <- dao.listSeasons
      ts <- dao.listTeams
      sch <- dao.loadLatestSchedule()
      du<-loadDisplayUser(rs)
      qw<-getQuoteWrapper(du)
    } yield {
      sch match {
        case Some(s)=>
          Ok (
            views.html.admin.logreg (
              du,qw,
              seasons = ls,
              teams = ts,
              features = features,
              normalizations = normalizations,
              form = PredictionModelForm.form,
              datePredictions = List.empty[(LocalDate, List[GamePrediction] )],
              teamPredictions = List.empty[TeamPredictionView],
              ps = LogisticPerformanceSummary.empty,
              sch = s,
              featureCoefficients = List.empty[(String, Double)],
              mode = "request"
            )
          )
        case None=>InternalServerError("Latest schedule data is unavailable")
      }
    }
  }

  def logisticRun() = silhouette.UserAwareAction.async { implicit rs =>
    PredictionModelForm.form.bindFromRequest.fold(
      form => {
        handleFormErrors( form)
      },
      data => {
        handleModelCalibrationAndPrediction(data)
      }
    )
  }

  private def handleModelCalibrationAndPrediction(data: PredictionModelForm.Data)(implicit rs: UserAwareRequest[DefaultEnv, AnyContent]) = {
    (for {
     du<-loadDisplayUser(rs)
     qw<-getQuoteWrapper(du)
   } yield {
     (du,qw)
   }).flatMap {
      case (d, q) => StatValueGameFeatureMapper.create(data.features, data.normalization, dao).flatMap { fm =>
      LogisticRegressionContext.selectTrainingSet(
        data.seasonsIncluded.map(_.toInt),
        data.excludeMonths.map(_.toInt),
        dao
      ).flatMap(gors => {
        val context: LogisticRegressionContext = LogisticRegressionContext.create(fm, StraightWinCategorizer, gors, dao)
        val resultLines = context.modelPerformance()
        val eventualTuple: Future[(Map[Long, Season], Map[Long, Team], Option[Schedule])] = for {
          seasons <- dao.listSeasons
          sch <- dao.loadLatestSchedule()
        } yield {
          (seasons.map(s => s.id -> s).toMap, sch.map(_.teamsMap).getOrElse(Map.empty[Long, Team]), sch)
        }
        eventualTuple.collect { case (seasonMap, teamMap, Some(s)) =>
          log.info(s"${data.predictFrom}, ${data.predictTo}")
          val datePredictions = (data.predictFrom, data.predictTo) match {
            case (Some(from), Some(to)) => context.predictDates(s, from, to)
            case (Some(from), None) => context.predictDates(s, from, from.plusDays(6))
            case (None, Some(to)) => context.predictDates(s, LocalDate.now(), to)
            case (None, None) => List.empty[(LocalDate, List[GamePrediction])]
          }
          val teamPredictions = context.predictTeams(s, data.predictTeams).map(tup => {
            TeamPredictionView(tup._1, tup._2.map(gp => PredictionView.create(s, gp)).filter(_.isDefined).map(_.get).sortBy(_.date.toEpochDay))
          })
          val perfSummary = LogisticPerformanceSummary(resultLines, seasonMap, teamMap)
          val form = PredictionModelForm.form.fill(data)

          Ok(views.html.admin.logreg(d, q, seasonMap.values.toList, teamMap.values.toList, features, normalizations, form, datePredictions, teamPredictions, perfSummary, s, context.showFeatureCoefficients, "response")
          )
        }
      })
    }
    }
  }

  private def handleFormErrors(form: Form[PredictionModelForm.Data])(implicit rs: UserAwareRequest[DefaultEnv, AnyContent]): Future[Result] = {
    for {
      ls <- dao.listSeasons
      ts <- dao.listTeams
      sch <- dao.loadLatestSchedule()
      du<-loadDisplayUser(rs)
      qw<-getQuoteWrapper(du)
    } yield {
      log.warn(s"Bad request $form")
      sch match {
        case Some(s)=>
          BadRequest(
            views.html.admin.logreg(
              du,qw,
              seasons = ls,
              teams = ts,
              features = features,
              normalizations = normalizations,
              form = form,
              datePredictions = List.empty[(LocalDate,List[GamePrediction])],
              teamPredictions = List.empty[TeamPredictionView],
              ps = LogisticPerformanceSummary.empty,
              sch = s,
              featureCoefficients = List.empty[(String, Double)],
              mode = "request"
            )
          )
        case None =>
          InternalServerError("Latest schedule data is unavailable")
      }
    }
  }
}

object LogisticPerformanceSummary {
  def performanceSplits[B](ls: List[LogisticResultLine], f: (LogisticResultLine) => B): List[LogisticPerformanceSplit[B]] = {
    ls.groupBy(f).map { case (key: B, data: List[LogisticResultLine]) => {
      data.foldLeft(LogisticPerformanceSplit(key)) { case (s, r) => {
        if (r.correct)
          s.copy(correct = s.correct + 1, avgLogLikelihood = (r.logLikelihood + s.avgLogLikelihood * s.n) / (s.n + 1))
        else
          s.copy(incorrect = s.incorrect + 1, avgLogLikelihood = (r.logLikelihood + s.avgLogLikelihood * s.n) / (s.n + 1))
      }
      }
    }
    }.toList
  }

  def empty = LogisticPerformanceSummary(List.empty[LogisticResultLine], Map.empty[Long, Season], Map.empty[Long, Team])
}

final case class LogisticPerformanceSummary(ls: List[LogisticResultLine], seasonMap: Map[Long, Season], teamMap: Map[Long, Team]) {
  private val result2SeasMonth = (l: LogisticResultLine) => (seasonMap(l.game.seasonId), l.game.date.withDayOfMonth(1))
  private val result2SeasAway = (l: LogisticResultLine) => (seasonMap(l.game.seasonId), teamMap(l.game.awayTeamId))
  private val result2SeasHome = (l: LogisticResultLine) => (seasonMap(l.game.seasonId), teamMap(l.game.homeTeamId))
  private val result2pct = (l: LogisticResultLine) => {
    math.floor(20 * math.max(l.homePct, l.awayPct)) * 5
  }

  val overallSplit: List[LogisticPerformanceSplit[String]] = LogisticPerformanceSummary.performanceSplits(ls, _ => "All")
  val seasonMonthSplits: List[LogisticPerformanceSplit[(Season, LocalDate)]] = LogisticPerformanceSummary.performanceSplits(ls, result2SeasMonth)
  val seasonTeamSplit: List[LogisticPerformanceSplit[(Season, Team)]] = combine(
    LogisticPerformanceSummary.performanceSplits(ls, result2SeasHome),
    LogisticPerformanceSummary.performanceSplits(ls, result2SeasAway)
  )
  val splitByPct: List[LogisticPerformanceSplit[Double]] = LogisticPerformanceSummary.performanceSplits(ls, result2pct)

  def combine[B](xs: List[LogisticPerformanceSplit[B]], ys: List[LogisticPerformanceSplit[B]]): List[LogisticPerformanceSplit[B]] = {
    val xm = xs.map(x => x.key -> x).toMap
    val ym = ys.map(y => y.key -> y).toMap
    (xm.keySet ++ ym.keySet).map(k => {
      (xm.get(k), ym.get(k)) match {
        case (Some(x), Some(y)) => x.combine(k, y)
        case (Some(x), _) => x
        case (_, Some(y)) => y
        case (_, _) => throw new IllegalArgumentException("")
      }
    }).toList
  }

}