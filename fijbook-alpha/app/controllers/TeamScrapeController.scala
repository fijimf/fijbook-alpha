package controllers

import java.time.LocalDateTime

import akka.actor.ActorRef
import akka.contrib.throttle.Throttler
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.dao.schedule.util.ScheduleUtil
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import forms.ScrapeOneTeamForm
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import cats.implicits._
import com.fijimf.deepfij.schedule.model.Schedule
import com.fijimf.deepfij.scraping.model.{ShortNameAndKeyByStatAndPage, TeamDetail, TestUrl}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class TeamScrapeController @Inject()(
                                      val controllerComponents: ControllerComponents,
                                      @Named("data-load-actor") teamLoad: ActorRef,
                                      @Named("throttler") throttler: ActorRef,
                                      val dao: ScheduleDAO,
                                      silhouette: Silhouette[DefaultEnv])
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)
  implicit val timeout: Timeout = Timeout(600.seconds)
  throttler ! Throttler.SetTarget(Some(teamLoad))

  def scrapeTeams(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    val userTag: String = "Scraper[" + rs.identity.name + "]"
    val aliasFuture = loadAliasMap()
    val shortNameFuture = masterShortName(List(1, 2, 3, 4, 5, 6, 7), 145)
    val teamMaster: Future[List[Team]] = for {
      aliasMap <- aliasFuture
      teamShortNames <- shortNameFuture
    } yield {
      logger.info(s"Loaded ${teamShortNames.size} short names.")
      scrapeKeyList(teamShortNames.toList, userTag, aliasMap).toList
    }

    teamMaster.onComplete{
      case Success(lst)=>
        val (good, bad) = lst.partition(t => t.name.trim.nonEmpty && t.nickname.trim.nonEmpty)
        logger.info(s"Saving ${good.size} teams")
        dao.saveTeams(good)
      case Failure(ex)=>
        logger.error("Failed loading teams")
    }
    Future.successful(Redirect(routes.DataController.browseTeams()).flashing("info" -> "Scraping teams"))

  }

  private def loadAliasMap(): Future[Map[String, String]] = {
    logger.info("Loading aliases from database")
    dao.listAliases.map(_.map(alias => alias.alias -> alias.key).toMap)
  }

  def scrapeKeyList(is: Iterable[(String, String)], userTag: String, aliasMap: Map[String, String]): Iterable[Team] = {
    val futureTeams: Iterable[Future[Option[Team]]] = is.map {
      case (key, shortName) =>
        (throttler ? TeamDetail(aliasMap.getOrElse(key, key), shortName, userTag))
          .mapTo[Either[Throwable, Team]]
          .map(_.fold(thr => None, t => Some(t)))
    }
    Await.result(Future.sequence(futureTeams), 600.seconds).toList.flatten //TODO
  }

  def masterShortName(pagination: List[Int], stat: Int): Future[Map[String, String]] = {
    pagination.foldLeft(Future.successful(Seq.empty[(String, String)]))((data: Future[Seq[(String, String)]], p: Int) => {
      for (
        t0 <- data;
        t1 <- (throttler ? ShortNameAndKeyByStatAndPage(stat, p)).mapTo[Either[Throwable, Seq[(String, String)]]].map(_.fold(
          thr => Seq.empty[(String, String)],
          seq => seq
        ))
      ) yield t0 ++ t1
    }).map(_.toMap)
  }

  def scrapeConferences(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    val userTag: String = "Scraper[" + rs.identity.name + "]"

    val basicKey: String => String = _.toLowerCase.replace(' ', '-')
    val dropThe: String => String = basicKey.andThen(_.replaceFirst("^the\\-", ""))
    val dropConference: String => String = basicKey.andThen(_.replaceFirst("\\-conference$", ""))
    val dropLeague: String => String = basicKey.andThen(_.replaceFirst("\\-league$", ""))
    val dropEllipsis: String => String = basicKey.andThen(_.replaceFirst("\\.\\.\\.$", ""))
    val tryConf: String => String = basicKey.andThen(_.replaceFirst("\\-athletic\\.\\.\\.$", "-athletic-conference"))
    val dropAthletic: String => String = basicKey.andThen(_.replaceFirst("\\-athletic\\.\\.\\.$", ""))
    val initials: String => String = basicKey.andThen(s => new String(s.split('-').map(_.charAt(0))))
    val tryAthletic: String => String = basicKey.andThen(_.replaceFirst("\\.\\.\\.$", "-athletic"))

    val transforms = List(basicKey, dropThe, dropConference, dropThe.andThen(dropConference), dropLeague, dropThe.andThen(dropLeague), initials, dropThe.andThen(initials), dropEllipsis, tryConf, dropAthletic, tryAthletic)
    logger.info("Loading preliminary team keys.")
    val teamList = Await.result(dao.listTeams, 600.seconds)

    val names = teamList.map(_.optConference.replaceFirst("Athletic Association$", "Athletic...")).toSet
    val conferences: List[Conference] = Conference(0L, "independents", "Independents", "Unknown", None, None, None, None, None, LocalDateTime.now(), userTag) :: names.map(n => {
      val candidate: Future[Option[String]] = Future.sequence(
        transforms.map(f => f(n)).toSet.map((k: String) => {
          logger.info("Trying " + k)
          val u = TestUrl("http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + k + ".70.png")
          (throttler ? u).mapTo[Option[Int]].map(oi => k -> oi)
        })).map(_.find(_._2 === Some(200)).map(_._1))

      val key = Await.result(candidate, Duration.Inf).getOrElse(n.toLowerCase.replace(' ', '-'))
      val smLogo = "http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + key + ".40.png"
      val lgLogo = "http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + key + ".70.png"
      Conference(0L, key, n.replaceFirst("\\.\\.\\.$", ""), "Low Major", Some(lgLogo), Some(smLogo), None, None, None, LocalDateTime.now(), userTag)
    }).toList
    dao.saveConferences(conferences).map(_ => Redirect(routes.AdminController.index()))
  }

  def seedConferenceMaps(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    val userTag: String = "Scraper[" + rs.identity.name + "]"

    (for {
      _ <- dao.deleteAllConferenceMaps()
      lcm <- ScheduleUtil.createConferenceMapSeeds(dao, userTag)
      _ <- dao.saveConferenceMaps(lcm)
    } yield {
      Redirect(routes.AdminController.index())
    }).recover {
      case ex: Exception => Redirect(routes.AdminController.index()).flashing("warn"->s"${ex.getMessage}")
    }
  }


  def conferenceTourneySolver(sch: Schedule): List[Game] = {
    List.empty[Game]
  }

  def neutralSiteSolver(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    val userTag: String = "Scraper[" + rs.identity.name + "]"
    dao.loadSchedules()
      .map(schedules =>
        schedules.flatMap(neutralUpdatesForSchedule)
      )
      .flatMap(gl =>
        dao.updateGames(gl)
      ).map(gs =>
      Redirect(routes.AdminController.index()).flashing("info" -> s"Updated ${gs.size} at a neutral site")
    )
  }

  private def neutralUpdatesForSchedule(sch: Schedule): List[Game] = {
    val locationData: Map[Long, Map[String, Int]] = sch.teamHomeGamesByLocation
    sch.games.foldLeft(List.empty[Option[Game]])((games: List[Option[Game]], game: Game) => {
      (for {
        location: String <- game.location
        homeGameSites: Map[String, Int] <- locationData.get(game.homeTeamId)
        timesAtLocation: Int <- homeGameSites.get(location)
        u: Game <- createNeutralUpdate(game, timesAtLocation)
      } yield {
        u
      }) :: games

    }).flatten
  }

  private def createNeutralUpdate(game: Game, timesAtLocation: Int): Option[Game] = {
    if (timesAtLocation > 3) {
      if (game.isNeutralSite) {
        Some(game.copy(isNeutralSite = false))
      } else {
        None
      }
    } else {
      if (game.isNeutralSite) {
        None
      } else {
        Some(game.copy(isNeutralSite = true))
      }
    }
  }

  def scrapeOne(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    val userTag: String = "Scraper[" + rs.identity.name + "]"
    ScrapeOneTeamForm.form.bindFromRequest.fold(
      form => {
        logger.error(form.errors.mkString("\n"))
        for {du <- loadDisplayUser(rs)
             qw <- getQuoteWrapper(du)
        } yield {
          BadRequest(views.html.admin.scrapeOneTeam(du,qw, form))
        }
      },
      data => {
        val fot: Future[Option[Team]] = (teamLoad ? TeamDetail(data.key, data.shortName, userTag))
          .mapTo[Either[Throwable, Team]]
          .map(_.fold(thr => None, t => Some(t)))
        fot.flatMap {
          case Some(t) =>
            dao.saveTeam(t).map(i => Redirect(routes.DataController.browseTeams()).flashing("info" -> ("Scrapeed " + data.shortName)))
          case None =>
            Future.successful(Redirect(routes.DataController.browseTeams()).flashing("info" -> ("Failed to scrape " + data.shortName)))
        }
      }
    )
  }

  def scrapeOneForm(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    for {du <- loadDisplayUser(rs)
         qw <- getQuoteWrapper(du)
    } yield {
      Ok(views.html.admin.scrapeOneTeam(du, qw, ScrapeOneTeamForm.form))
    }
  }


}