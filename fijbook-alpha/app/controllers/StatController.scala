package controllers

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.implicits._
import com.cibo.evilplot.geometry.Extent
import com.cibo.evilplot.numeric.Point
import com.cibo.evilplot.plot.aesthetics.DefaultTheme.DefaultColors
import com.cibo.evilplot.plot.aesthetics._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.nstats.Analysis
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import javax.imageio.ImageIO
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import play.api.{Logger, mvc}

class StatController @Inject()(
                                val controllerComponents: ControllerComponents,
                                val dao: ScheduleDAO,
                                cache: AsyncCacheApi,
                                silhouette: Silhouette[DefaultEnv]
                              )
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)

  def scaleDefaultTheme(scale: Double): Theme = {
    val f = DefaultTheme.DefaultFonts

    val e = DefaultTheme.DefaultElements

    Theme(
      fonts = f.copy(
        titleSize = f.titleSize * scale,
        labelSize = f.labelSize * scale,
        annotationSize = f.annotationSize * scale,
        tickLabelSize = f.tickLabelSize * scale,
        legendLabelSize = f.legendLabelSize * scale,
        facetLabelSize = f.facetLabelSize * scale
      ),
      colors = DefaultColors,
      elements = e.copy(
        strokeWidth = e.strokeWidth * scale,
        pointSize = e.pointSize * scale,
        gridLineSize = e.gridLineSize * scale,
        barSpacing = e.barSpacing * scale,
        clusterSpacing = e.clusterSpacing * scale,
        tickThickness = e.tickThickness * scale,
        tickLength = e.tickLength * scale
      )
    )
  }

  def timeSeries(seasonId: Long, key: String, teamId: Long, width: Double = 175.0, height: Double = 125.0): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    import com.cibo.evilplot.plot._

    implicit val theme: Theme = scaleDefaultTheme(1.0)
    for {
      du <- loadDisplayUser(request)
      qw <- getQuoteWrapper(du)
      lst <- dao.findXStatsTimeSeries(seasonId, teamId, key)
    } yield {
      val plot: Plot = LinePlot(lst.flatMap(x => x.value match {
        case Some(v) => Some(Point(x.date.toEpochDay.toDouble, v))
        case _ => None
      }))
      returnBufferedImage(plot.render().asBufferedImage)
    }
  }


  def histogram(seasonId: Long, key: String, yyyymmdd: Int, width: Double = 175.0, height: Double = 125.0): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    import com.cibo.evilplot.plot._
    implicit val theme: Theme = scaleDefaultTheme(1.0)

    for {
      du <- loadDisplayUser(request)
      qw <- getQuoteWrapper(du)
      lst <- dao.findXStatsSnapshot(seasonId, LocalDate.parse(yyyymmdd.toString, DateTimeFormatter.ofPattern("yyyyMMdd")), key)
    } yield {
      val plot: Plot = Histogram(lst.flatMap(_.value))
        .xLabel(Analysis.models.find(_._2.key === key).map(_._1).getOrElse(""), size = Some(4))
        .yLabel("Number of Teams")
        .yGrid()
        .xAxis(tickCount = Some(10))
        .yAxis()

      returnBufferedImage(plot.render().asBufferedImage)
    }
  }


  def returnBufferedImage(bi: BufferedImage, fmt: String = "png"): mvc.Result = {
    val baos = new ByteArrayOutputStream()
    ImageIO.write(bi, fmt, baos)
    Ok(baos.toByteArray).as(s"image/$fmt")
  }


}
