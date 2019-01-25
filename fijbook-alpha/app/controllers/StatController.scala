package controllers

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.implicits._
import com.cibo.evilplot.colors.{Color, HEX, HTMLNamedColors}
import com.cibo.evilplot.numeric.Point
import com.cibo.evilplot.plot.aesthetics.DefaultTheme.DefaultColors
import com.cibo.evilplot.plot.aesthetics._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.nstats.Analysis
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import controllers.Utils._
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
        .xGrid()
        .yGrid()
        .xAxis(labelFormatter = Some((d: Double) => LocalDate.ofEpochDay(d.longValue()).fmt("MMM-dd")))
        .yAxis()

      returnBufferedImage(plot.render().asBufferedImage)
    }
  }


  def histogram(seasonId: Long, key: String, yyyymmdd: Int, teamId: Long, width: Double = 175.0, height: Double = 125.0): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    import com.cibo.evilplot.plot._
    implicit val theme: Theme = scaleDefaultTheme(1.0)

    for {
      du <- loadDisplayUser(request)
      qw <- getQuoteWrapper(du)
      team <- dao.findTeamById(teamId)
      lst <- dao.findXStatsSnapshot(seasonId, LocalDate.parse(yyyymmdd.toString, DateTimeFormatter.ofPattern("yyyyMMdd")), key, false)
    } yield {
      val mean = lst.head.mean.getOrElse(Double.NaN)
      val stdDev = lst.head.stdDev.getOrElse(Double.NaN)
      val tx = lst.find(_.teamId === teamId).flatMap(_.value).getOrElse(Double.NaN)

      val hist = Histogram(lst.flatMap(_.value))
        .xLabel(Analysis.models.find(_._2.key === key).map(_._1).getOrElse(""))
        .xLabel("\u00B5 = %.3f  \u03C3 = %.3f".format(mean, stdDev))
        .xLabel(s"${team.map(_.name).getOrElse("")} = %.3f".format(tx))
        .yLabel("Number of Teams")
        .yGrid()
        .xAxis(tickCount = Some(10))
        .yAxis()

      val histPlus: Plot = List(-4, -3, -2, -1, 0, 1, 2, 3, 4)
        .map(_ * stdDev + mean)
        .filter(x => hist.xbounds.isInBounds(x))
        .foldLeft(hist) {
          case (p, xl) =>
            if (xl === mean) {
              p.vline(xl, thickness = theme.elements.strokeWidth, color = HTMLNamedColors.darkGray)
            } else {
              p.vline(xl, thickness = 0.5 * theme.elements.strokeWidth, color = HTMLNamedColors.lightGray)

            }
        }
      val teamColor: Color = team.flatMap(_.primaryColor).map(c => HEX(c)).getOrElse(HTMLNamedColors.lightPink).copy(opacity = 0.65)
      val plot = histPlus.vline(tx, color = teamColor, thickness = 4 * theme.elements.strokeWidth)

      returnBufferedImage(plot.render().asBufferedImage)
    }
  }


  def returnBufferedImage(bi: BufferedImage, fmt: String = "png"): mvc.Result = {
    val baos = new ByteArrayOutputStream()
    ImageIO.write(bi, fmt, baos)
    Ok(baos.toByteArray).as(s"image/$fmt")
  }


  def showStat(key: String) = play.mvc.Results.TODO

  def showStat(key: String, yyyymmdd:String) = play.mvc.Results.TODO

  def stats() = play.mvc.Results.TODO

  def showStatSnapshot(key: String, yyyymmdd: String) = play.mvc.Results.TODO
}