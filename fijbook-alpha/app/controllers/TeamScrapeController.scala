package controllers

import java.time.LocalDateTime

import akka.actor.ActorRef
import akka.contrib.throttle.Throttler
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.scraping.modules.scraping.requests.TeamDetail
import com.fijimf.deepfij.scraping.{ShortNameAndKeyByStatAndPage, TestUrl}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Silhouette
import forms.ScrapeOneTeamForm
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Controller}
import utils.DefaultEnv

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TeamScrapeController @Inject()(@Named("data-load-actor") teamLoad: ActorRef, @Named("throttler") throttler: ActorRef, val teamDao: ScheduleDAO, silhouette: Silhouette[DefaultEnv], val messagesApi: MessagesApi) extends Controller with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)
  implicit val timeout = Timeout(600.seconds)
  throttler ! Throttler.SetTarget(Some(teamLoad))

  def scrapeTeams(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Loading aliases from database")
    val aliasMap: Map[String, String] = Await.result(teamDao.listAliases.map(_.map(alias => alias.alias -> alias.key)), 600.seconds).toMap

    logger.info("Loading preliminary team keys.")
    val teamShortNames: Future[Map[String, String]] = masterShortName(List(1, 2, 3, 4, 5, 6, 7), 145)

    logger.info("Loading team detail")

    val teamMaster: Future[List[Team]] = teamShortNames.map((tsn: Map[String, String]) => {
      tsn.toList.grouped(4).map((is: Iterable[(String, String)]) => {
        val userTag: String = "Scraper[" + rs.identity.name.getOrElse("???") + "]"
        scrapeKeyList(is, userTag, aliasMap)
      }).flatten.toList
    })

    teamMaster.flatMap(lst => {
      val (good, bad) = lst.partition(t => t.name.trim.nonEmpty && t.nickname.trim.nonEmpty)
      good.foreach(t => {
        logger.info("Saving " + t.key)
        teamDao.saveTeam(t)
      })
      Future.successful(Redirect(routes.DataController.browseTeams()).flashing("info" -> ("Loaded " + good.size + " Teams")))
    })

  }

  def scrapeKeyList(is: Iterable[(String, String)], userTag: String, aliasMap: Map[String, String]): Iterable[Team] = {
    val futureTeams: Iterable[Future[Option[Team]]] = is.map {
      case (key, shortName) => {
        (throttler ? TeamDetail(aliasMap.getOrElse(key, key), shortName, userTag))
          .mapTo[Either[Throwable, Team]]
          .map(_.fold(thr => None, t => Some(t)))
      }
    }
    Await.result(Future.sequence(futureTeams), 600.seconds).flatten
  }

  def masterShortName(pagination: List[Int], stat: Int): Future[Map[String, String]] = {
    pagination.foldLeft(Future.successful(Seq.empty[(String, String)]))((data: Future[Seq[(String, String)]], p: Int) => {
      for (
        t0 <- data;
        t1 <- (throttler ? ShortNameAndKeyByStatAndPage(stat, p)).mapTo[Either[Throwable, Seq[(String, String)]]].map(_.fold(
          thr => Seq.empty,
          seq => seq
        ))
      ) yield t0 ++ t1
    }).map(_.toMap)
  }

  def scrapeConferences() = silhouette.SecuredAction.async { implicit rs =>
    val userTag: String = "Scraper[" + rs.identity.name.getOrElse("???") + "]"

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
    val teamList = Await.result(teamDao.listTeams, 600.seconds)

    val names = teamList.map(_.optConference.replaceFirst("Athletic Association$", "Athletic...")).toSet
    val conferences: List[Conference] = Conference(0L, "independents", "Independents", None, None, None, None, None, false, LocalDateTime.now(), userTag) :: names.map(n => {
      val candidate: Future[Option[String]] = Future.sequence(
        transforms.map(f => f(n)).toSet.map((k: String) => {
          logger.info("Trying " + k)
          val u = TestUrl("http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + k + ".70.png")
          (throttler ? u).mapTo[Option[Int]].map(oi => k -> oi)
        })).map(_.filter(_._2 == Some(200)).headOption.map(_._1))

      val key = Await.result(candidate, Duration.Inf).getOrElse(n.toLowerCase.replace(' ', '-'))
      val smLogo = "http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + key + ".40.png"
      val lgLogo = "http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/ncaa/images/logos/conferences/" + key + ".70.png"
      Conference(0L, key, n.replaceFirst("\\.\\.\\.$", ""), Some(lgLogo), Some(smLogo), None, None, None, false, LocalDateTime.now(), userTag)
    }).toList
    teamDao.saveConferences(conferences).map(_ => Redirect(routes.AdminController.index()))
  }

  def seedConferenceMaps() = silhouette.SecuredAction.async { implicit rs =>
    val userTag: String = "Scraper[" + rs.identity.name.getOrElse("???") + "]"
    (for (
      sl <- teamDao.listSeasons;
      tl <- teamDao.listTeams;
      cl <- teamDao.listConferences
    ) yield {
      val confNameMap = cl.map(c => c.name -> c).toMap ++ cl.map(c => c.name.replace("Conference", "").trim -> c) ++ cl.map(c => c.name.replace("The", "").trim -> c)
      for (
        s <- sl;
        t <- tl
      ) yield {
        val nameKey = t.optConference.replaceFirst("Athletic Association$", "Athletic...").replaceFirst("\\.\\.\\.$", "")
        val conf = confNameMap.get(nameKey).orElse(confNameMap.get("Independents"))
        ConferenceMap(0L, s.id, conf.map(_.id).getOrElse(0L), t.id, false, LocalDateTime.now(), userTag)
      }
    }).flatMap(lcm => {
      Future.sequence(lcm.map(cm => teamDao.saveConferenceMap(cm)))
    }).map(_ => Redirect(routes.AdminController.index()))
  }

  def conferenceTourneySolver(sch: Schedule): List[Game] = {
    List.empty[Game]
  }

  def neutralSiteSolver() = silhouette.SecuredAction.async { implicit rs =>
    val userTag: String = "Scraper[" + rs.identity.name.getOrElse("???") + "]"
    teamDao.loadSchedules()
      .map(schedules =>
        schedules.flatMap(neutralUpdatesForSchedule)
      )
      .flatMap(gl =>
        teamDao.updateGames(gl)
      ).map(gs =>
      Redirect(routes.AdminController.index()).flashing("info" -> s"Updated ${gs.size} at a neutral site")
    )
  }

  private def neutralUpdatesForSchedule(sch: Schedule): List[Game] = {
    val locationData: Map[Long, Map[String, Int]] = sch.teamHomeGamesByLocation
    sch.games.foldLeft(List.empty[Option[Game]])((games: List[Option[Game]], game: Game) => {
      ( for {
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

  def scrapeOne() = silhouette.SecuredAction.async { implicit rs =>
    val userTag: String = "Scraper[" + rs.identity.name.getOrElse("???") + "]"
    ScrapeOneTeamForm.form.bindFromRequest.fold(
      form => {
        logger.error(form.errors.mkString("\n"))
        Future.successful(BadRequest(views.html.admin.scrapeOneTeam(rs.identity, form)))
      },
      data => {
        val fot: Future[Option[Team]] = (teamLoad ? TeamDetail(data.key, data.shortName, userTag))
          .mapTo[Either[Throwable, Team]]
          .map(_.fold(thr => None, t => Some(t)))
        fot.flatMap {
          case Some(t) =>
            teamDao.saveTeam(t).map(i => Redirect(routes.DataController.browseTeams()).flashing("info" -> ("Scrapeed " + data.shortName)))
          case None =>
            Future.successful(Redirect(routes.DataController.browseTeams()).flashing("info" -> ("Failed to scrape " + data.shortName)))
        }
      }
    )
  }

  def scrapeOneForm() = silhouette.SecuredAction.async { implicit rs =>
    Future.successful(Ok(views.html.admin.scrapeOneTeam(rs.identity, ScrapeOneTeamForm.form)))
  }


}