package controllers

import java.time.{LocalDateTime, ZoneId, ZoneOffset}

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.RssFeedUpdateService
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.silhouette.utils.DefaultEnv
import forms._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success}

class DataController @Inject()(
                                val controllerComponents: ControllerComponents,
                                val dao: ScheduleDAO,
                                val rssUpdater: RssFeedUpdateService,
                                silhouette: Silhouette[DefaultEnv]
                              )
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(getClass)

  def saveTeam: Action[AnyContent] = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    (for {
      du <- loadDisplayUser(request)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) =>

      EditTeamForm.form.bindFromRequest.fold(
        form => {
          val formId: Int = form("id").value.getOrElse("0").toInt
          dao.findTeamById(formId).map {
            case Some(t) => BadRequest(views.html.admin.editTeam(d,q ,t, form))
            case None => Redirect(routes.DataController.browseTeams()).flashing("error" -> ("Bad request with an unknown id: " + form("id")))
          }
        },
        data => {
          val t = Team(
            data.id,
            data.key,
            data.name,
            data.longName,
            data.nickname,
            data.optConference,
            data.logoLgUrl,
            data.logoSmUrl,
            data.primaryColor,
            data.secondaryColor,
            data.officialUrl,
            data.officialTwitter,
            data.officialUrl,
            LocalDateTime.now(),
            request.identity.userID.toString)
          dao.saveTeam(t).map(i => Redirect(routes.DataController.browseTeams()).flashing("info" -> ("Saved " + data.name)))
        }
      )
    }
  }

  def editTeam(id: Long): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Loading preliminary team keys.")
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) =>
      dao.findTeamById(id).map {
        case Some(t) => Ok(views.html.admin.editTeam(d, q, t, EditTeamForm.form.fill(EditTeamForm.team2Data(t))))
        case None => Redirect(routes.DataController.browseTeams()).flashing(FlashUtil.warning("No team found with id " + id))
      }
    }
  }

  def browseTeams(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Loading preliminary team keys.")
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) =>
      dao.listTeams.map(ot => Ok(views.html.admin.browseTeams(d, q, ot.sortBy(_.name))))
    }
  }

  def createSeason(): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => Future.successful(Ok(views.html.admin.createSeason(d, q, EditSeasonForm.form)))
    }

  }

  def saveSeason() = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) =>
    EditSeasonForm.form.bindFromRequest.fold(
      form => {
        logger.error(form.errors.mkString("\n"))
        Future.successful(BadRequest(views.html.admin.createSeason(d, q, form)))
      },
      data => {
        val s = Season(data.id, data.year)
        val future: Future[Season] = dao.saveSeason(s)
        future.onComplete {
          case Success(ss) => logger.info("Saved season: " + ss)
          case Failure(thr) => logger.error("Failed to save season: " + s, thr)
        }
        future.map(i => Redirect(routes.AdminController.index()).flashing("info" -> ("Created empty season for " + data.year)))
      }
    )
    }
  }

  def browseQuotes() = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => dao.listQuotes.map(qs => Ok(views.html.admin.browseQuotes(d,q, qs))) }
  }

  def createQuote() = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => Future.successful(Ok(views.html.admin.createQuote(d,q, EditQuoteForm.form.fill(EditQuoteForm.Data(0L, "", None, None, None)))))
    }
  }

  def deleteQuote(id: Long) = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => dao.deleteQuote(id).map(n => Redirect(routes.DataController.browseQuotes()).flashing("info" -> ("Quote " + id + " deleted")))
    }
  }

  def editQuote(id: Long) = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, qq) => dao.findQuoteById(id).map {
      case Some(q) => Ok(views.html.admin.createQuote(d,qq, EditQuoteForm.form.fill(EditQuoteForm.Data(id, q.quote, q.source, q.url, q.key))))
      case None => Redirect(routes.DataController.browseQuotes()).flashing("warn" -> ("No quote found with id " + id))
    }
    }
  }

  def saveQuote() = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => EditQuoteForm.form.bindFromRequest.fold(
      form => {
        logger.error(form.errors.mkString("\n"))
        Future.successful(BadRequest(views.html.admin.createQuote(d,q, form)))
      },
      data => {
        val q = Quote(data.id, data.quote, data.source, data.url, data.key)
        val future: Future[Quote] = dao.saveQuote(q)
        future.onComplete {
          case Success(q) => logger.info(s"Saved quote: '${q.quote}' (${q.id})")
          case Failure(thr) => logger.error(s"Failed saving quote $q", thr)
        }
        val flashMsg = if (q.id == 0) "Created quote " + q.id else "Updated quote" + q.id
        future.map(i => Redirect(routes.DataController.browseQuotes()).flashing("info" -> flashMsg))
      }
    )
    }
  }

  def bulkCreateQuote() = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => Future.successful(Ok(views.html.admin.bulkCreateQuote(d,q, BulkEditQuoteForm.form.fill(BulkEditQuoteForm.Data("")))))
    }
  }

  def bulkSaveQuote() = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => BulkEditQuoteForm.form.bindFromRequest.fold(
      form => {
        logger.error(form.errors.mkString("\n"))
        Future.successful(BadRequest(views.html.admin.bulkCreateQuote(d,q, form)))
      },
      data => {
        val qs = data.quotes.split("\n").map(_.trim.split("\t").toList).flatMap {
          case q :: s :: Nil => Some(Quote(0l, q, Some(s), None, None))
          case q :: s :: u :: Nil => Some(Quote(0l, q, Some(s), Some(u), None))
          case q :: s :: u :: k :: Nil => Some(Quote(0l, q, Some(s), Some(u), Some(k)))
          case _ => None
        }.toList
        val future: Future[List[Quote]] = dao.saveQuotes(qs)
        future.onComplete {
          case Success(q) => logger.info(s"Saved ${q.size} quotes.")
          case Failure(thr) => logger.error(s"Failed bulk saving quotes", thr)
        }
        val flashMsg = s"Bulk saved quotes."
        future.map(i => Redirect(routes.DataController.browseQuotes()).flashing("info" -> flashMsg))
      }
    )
    }
  }

  def createAlias() = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => Future.successful(Ok(views.html.admin.createAlias(d,q, EditAliasForm.form)))
    }
  }

  def browseAliases() = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => dao.listAliases.map(qs => Ok(views.html.admin.browseAliases(d,q, qs)))
    }
  }

  def saveAlias() = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => EditAliasForm.form.bindFromRequest.fold(
      form => {
        logger.error(form.errors.mkString("\n"))
        Future.successful(BadRequest(views.html.admin.createAlias(d,q, form)))
      },
      data => {
        val q = Alias(data.id, data.alias, data.key)
        val future: Future[Alias] = dao.saveAlias(q)
        future.map(i => Redirect(routes.DataController.browseAliases()).flashing("info" -> ("Aliased  " + data.alias + " to " + data.key)))
      }
    )
    }
  }

  def editAlias(id: Long) = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, qq) => dao.findAliasById(id).map {
      case Some(q) => Ok(views.html.admin.createAlias(d,qq, EditAliasForm.form.fill(EditAliasForm.Data(id, q.alias, q.key))))
      case None => Redirect(routes.DataController.browseAliases()).flashing("warn" -> ("No alias found with id " + id))
    }
    }
  }

  def deleteAlias(id: Long) = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => dao.deleteAlias(id).map(n => Redirect(routes.DataController.browseAliases()).flashing("info" -> ("Alias " + id + " deleted")))
    }
  }

  def browseConferences() = silhouette.SecuredAction.async { implicit rs =>
    logger.info("Loading preliminary team keys.")
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) =>
      dao.listConferences.map(oc => Ok(views.html.admin.browseConferences(d,q, oc.sortBy(_.name))))
    }
  }

  def deleteConference(id: Long) = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => dao.deleteConference(id).map(n => Redirect(routes.DataController.browseConferences()).flashing("info" -> ("Conference " + id + " deleted")))
    }
  }

  def editConference(id: Long) = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => dao.findConferenceById(id).map {
      case Some(c) => Ok(views.html.admin.editConference(d,q, c, EditConferenceForm.form.fill(EditConferenceForm.Data(id, c.key, c.name, c.logoSmUrl, c.logoLgUrl, c.officialUrl, c.officialTwitter, c.officialFacebook))))
      case None => Redirect(routes.DataController.browseQuotes()).flashing("warn" -> ("No quote found with id " + id))
    }
    }
  }

  def saveConference() = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => EditConferenceForm.form.bindFromRequest.fold(
      form => {
        val formId: Int = form("id").value.getOrElse("0").toInt
        val fot: Future[Option[Conference]] = dao.findConferenceById(formId)
        fot.map(ot => ot match {
          case Some(t) => BadRequest(views.html.admin.editConference(d,q, t, form))
          case None => Redirect(routes.DataController.browseConferences()).flashing("error" -> ("Bad request with an unknown id: " + form("id")))
        })
      },
      data => {
        val q = Conference(data.id, data.key, data.name, data.logoLgUrl, data.logoSmUrl, data.officialUrl, data.officialTwitter, data.officialFacebook, LocalDateTime.now(),
          d.user.get.userID.toString)
        val future: Future[Conference] = dao.saveConference(q)
        future.onComplete {
          case Success(conference) => logger.info(s"Saved conference '${conference.name}")
          case Failure(thr) => logger.error(s"Failed saving $q with error ${thr.getMessage}", thr)
        }
        future.map(i => Redirect(routes.DataController.browseConferences()).flashing("info" -> ("Saved " + data.name)))
      }
    )
    }
  }


  def initializeAliases() = silhouette.SecuredAction.async { implicit request =>
    val lines: List[String] = Source.fromInputStream(getClass.getResourceAsStream("/aliases.txt")).getLines.toList.map(_.trim).filterNot(_.startsWith("#")).filter(_.length > 0)
    val aliases = lines.flatMap(l => {
      val parts = l.trim.split("\\s+")
      if (parts.size == 2) {
        Some(Alias(0L, parts(0), parts(1)))
      } else {
        None
      }
    })
    dao.saveAliases(aliases).map(
      _ => Redirect(routes.DataController.browseAliases())
    )
  }


  def deleteTeam(id: Long) = play.mvc.Results.TODO

  def browseConferenceMap(seasonId: Long) = play.mvc.Results.TODO

  def browseGames(id: Long, query: Option[String]) = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => dao.loadSchedules().map(_.find(_.season.id == id) match {
      case Some(sch) =>
        val infos = sch.gameResults.map {
          case (g: Game, or: Option[Result]) => {
            GameInfo(
              g.seasonId,
              g.id,
              or.map(_.id),
              g.datetime,
              sch.teamsMap(g.homeTeamId).name,
              or.map(_.homeScore),
              sch.teamsMap(g.awayTeamId).name,
              or.map(_.awayScore),
              or.map(_.periods),
              g.location.getOrElse(""),
              g.isNeutralSite,
              g.tourneyKey.getOrElse(""),
              g.homeTeamSeed,
              g.awayTeamSeed,
              g.sourceKey,
              g.updatedAt
            )
          }
        }.sortBy(g => (g.source, g.datetime.toEpochSecond(ZoneOffset.of("Z")), g.id))

        query match {
          case Some(str) => Ok(views.html.admin.browseGames(d,q, infos.filter(_.matches(str)), Some(str)))
          case None => Ok(views.html.admin.browseGames(d,q, infos, None))
        }

      case None => Redirect(routes.AdminController.index()).flashing("warn" -> ("No schedule found with id " + id))
    })
    }
  }

  def lockSeason(id: Long) = play.mvc.Results.TODO

  def deleteSeason(id: Long) = play.mvc.Results.TODO

  def editGame(id: Long) = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) => for {
      ot <- dao.gamesById(id)
      lt <- dao.listTeams
    } yield {
      ot match {
        case Some(c) =>
          val teamData = lt.map(t => t.id.toString -> t.name).sortBy(_._2)
          Ok(views.html.admin.editGame(d,q, c._1, EditGameForm.form.fill(EditGameForm.team2Data(c._1, c._2)), teamData))
        case None => Redirect(routes.DataController.browseQuotes()).flashing("warn" -> ("No quote found with id " + id))
      }
    }
    }
  }


  def saveGame = silhouette.SecuredAction.async { implicit rs =>
    (for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      (du, qw)
    }).flatMap { case (d, q) =>  dao.listTeams.flatMap(td => {
      val teamData = td.map(t => t.id.toString -> t.name).sortBy(_._2)
      EditGameForm.form.bindFromRequest.fold(
        form => {
          val formId: Int = form("id").value.getOrElse("0").toInt
          val formSeasonId: Int = form("seasonId").value.getOrElse("0").toInt
          val fot: Future[Option[(Game, Option[Result])]] = dao.gamesById(formId)
          fot.map {
            case Some(tup) => BadRequest(views.html.admin.editGame(d,q, tup._1, form, teamData))
            case None => Redirect(routes.DataController.browseGames(formSeasonId, None)).flashing("error" -> ("Bad request with an unknown id: " + form("id")))
          }
        },
        data => {
          val gr: (Game, Option[Result]) = data.toGameResult(rs.identity.userID.toString)
          val future: Future[Option[Game]] = dao.saveGameResult(gr._1, gr._2)
          future.onComplete {
            case Success(conference) => logger.info(s"Saved game '${gr._1.id}")
            case Failure(thr) => logger.error(s"Failed saving ${gr._1.id} with error ${thr.getMessage}", thr)
          }
          future.map(i => Redirect(routes.DataController.browseGames(data.seasonId, None)).flashing("info" -> ("Saved " + data.id)))
        }
      )
    })}
  }

  def deleteGame(id: Long) = silhouette.SecuredAction.async {
    implicit rs =>
      dao.deleteGames(List(id)).map(n => Redirect(routes.AdminController.index()).flashing("info" -> ("Conference " + id + " deleted")))
  }

  def browseRssFeeds() = silhouette.SecuredAction.async { implicit rs =>
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      feeds <- dao.listRssFeeds()
      items <- dao.listRssItems()
    } yield {
      val groupedItems: Map[Long, List[RssItem]] = items.groupBy(_.rssFeedId)
      val feedStatuses =feeds.map(f => {
        val feedItems: List[RssItem] = groupedItems.getOrElse(f.id, List.empty[RssItem])

        feedItems match {
          case Nil => RSSFeedStatus(f, None, None, 0, 0)
          case _ => RSSFeedStatus(f,
            Some(feedItems.maxBy(_.publishTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).publishTime),
            Some(feedItems.maxBy(_.recordedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).recordedAt),
            feedItems.size,
            feedItems.count(_.publishTime.isAfter(LocalDateTime.now.minusWeeks(1)))
          )
        }

      })
      Ok(views.html.admin.browseRssFeeds(du, qw, feedStatuses))
    }
  }

  def createRssFeed() = silhouette.SecuredAction.async { implicit rs =>
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      Ok(views.html.admin.createRssFeed(du,qw, EditRssFeedForm.form))
    }
  }

  def saveRssFeed() = silhouette.SecuredAction.async { implicit rs =>
    EditRssFeedForm.form.bindFromRequest.fold(
      form => {
        logger.error(form.errors.mkString("\n"))
        for {
          du <- loadDisplayUser(rs)
          qw <- getQuoteWrapper(du)
        } yield {
          (BadRequest(views.html.admin.createRssFeed(du,qw, form)))
        }
      },
      data => {
        for {
          _<-dao.saveRssFeed(RssFeed(data.id, data.name, data.url))
        } yield {
          Redirect(routes.DataController.browseRssFeeds()).flashing("info" -> ("Created feed " + data.name + " at " + data.url))
        }
      })
  }

  def editRssFeed(id: Long) = silhouette.SecuredAction.async { implicit rs =>
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      feed <- dao.findRssFeedById(id)
    } yield {
      feed match {
        case Some(f)=>
          Ok(views.html.admin.createRssFeed(du,qw,EditRssFeedForm.form.fill(EditRssFeedForm.Data(id, f.name, f.url))))
        case None=>Redirect(routes.DataController.browseRssFeeds()).flashing(FlashUtil.warning("RSS Feed not found"))
      }
    }
  }

  def deleteRssFeed(id: Long) = silhouette.SecuredAction.async { implicit rs =>
    for {
      n<-dao.deleteRssItemsByFeedId(id)
      _<-dao.deleteRssFeed(id)
    } yield {
      Redirect(routes.DataController.browseRssFeeds).flashing(FlashUtil.info(s"Deleted RSS feed $id and $n items"))
    }
  }

  def pollRssFeed(id: Long) = silhouette.SecuredAction.async { implicit rs =>
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
      items<-rssUpdater.updateFeed(id)
    } yield {
      Redirect(routes.DataController.browseRssFeeds).flashing(FlashUtil.info(s"Updated RSS feed $id and got ${items.size} items"))
    }


  }

}