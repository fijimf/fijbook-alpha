package com.fijimf.deepfij.models.services

import java.time.{LocalDate, LocalDateTime}
import javax.inject.Inject

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.scraping.ScoreboardByDateReq
import com.fijimf.deepfij.scraping.modules.scraping.EmptyBodyException
import com.fijimf.deepfij.scraping.modules.scraping.model.{GameData, ResultData}
import com.google.inject.name.Named
import controllers._
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer.{Email, MailerClient}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class ScheduleUpdateServiceImpl @Inject()(dao: ScheduleDAO, mailerClient: MailerClient,@Named("data-load-actor") teamLoad: ActorRef, @Named("throttler") throttler: ActorRef) extends ScheduleUpdateService {
  val logger = Logger(this.getClass)

  implicit val timeout = Timeout(600.seconds)
  val activeYear = 2017

  def update(optDates:Option[List[LocalDate]]=None, mailReport:Boolean = false) {
    val startTime = LocalDateTime.now()
    dao.findSeasonByYear(activeYear).map {
      case None =>
        logger.warn(s"Schedule not found for year $activeYear. Cannot run update")
        if (mailReport) {
          mailerClient.send(Email(
            subject = Messages("email.already.signed.up.subject"),
            from = Messages("email.from"),
            to = Seq(data.email),
            bodyText = Some(views.txt.silhouette.emails.alreadySignedUp(user, url).body),
            bodyHtml = Some(views.html.silhouette.emails.alreadySignedUp(user, url).body)
          ))
        }
      case Some(s) =>
        if (dao.checkAndSetLock(s.id)) {
          val updatedBy: String = "Scraper[Updater]"
          scrapeSeasonGames(s, optDates, updatedBy).onComplete {
            case Success(ssr: SeasonScrapeResult) =>
              logger.info("Schedule update scrape complete.")
              if (ssr.unmappedTeamCount.nonEmpty) {
                logger.info("The following teams had no games mapped")
                ssr.unmappedTeamCount.foreach((tuple: (String, Int)) => if (tuple._2 > 9) println(tuple._1 + "\t" + tuple._2))
              }
              dao.unlockSeason(s.id)
            case Failure(thr) =>
              logger.error("Schedule update scrape failed.", thr)
              dao.unlockSeason(s.id)
          }
        }
    }
  }

  def completeScrape(seasonId: Long): Future[Int] = {
    dao.unlockSeason(seasonId)
  }


  def scrapeSeasonGames(season: Season, optDates:Option[List[LocalDate]], updatedBy: String): Future[SeasonScrapeResult] = {
    dao.listAliases.flatMap(aliasDict => {
      dao.listTeams.flatMap(teamDictionary => {
        val dateList: List[LocalDate] = optDates.getOrElse(season.dates).filter(d => season.status.canUpdate(d))
        Await.result(Future.sequence(dateList.map(dd => dao.clearGamesByDate(dd))), 15.seconds)
        val map: List[Future[(LocalDate, GameScrapeResult)]] = dateList.map(d => scrape(season.id, updatedBy, teamDictionary, aliasDict, d).map(d -> _))
        Future.sequence(map).map(lgsr => SeasonScrapeResult(lgsr))
      })
    })
  }

  def scrape(seasonId: Long, updatedBy: String, teams: List[Team], aliases: List[Alias], d: LocalDate): Future[GameScrapeResult] = {
    val teamDict = teams.map(t => t.key -> t).toMap
    val aliasDict = aliases.filter(a => teamDict.contains(a.key)).map(a => a.alias -> teamDict(a.key))

    val masterDict = teamDict ++ aliasDict
    logger.info("Loading date " + d)
    val future: Future[Either[Throwable, List[GameData]]] = (throttler ? ScoreboardByDateReq(d)).mapTo[Either[Throwable, List[GameData]]]
    val results: Future[List[GameMapping]] = future.map(_.fold(
      thr => {
        if (thr == EmptyBodyException) {
          logger.warn("For date " + d + " scraper returned an empty body")
        } else {
          logger.error("For date " + d + " scraper returned an exception ", thr)
        }
        List.empty[GameMapping]
      },
      lgd => lgd.map(gameDataToGame(seasonId, updatedBy, masterDict, _))
    ))
    results.flatMap(gameList => {
      val z = Future.successful(GameScrapeResult())
      gameList.foldLeft(z)((fgsr: Future[GameScrapeResult], mapping: GameMapping) => {
        fgsr.flatMap(_.acc(dao, mapping))
      })

    })

  }


  def gameDataToGame(seasonId: Long, updatedBy: String, teamDict: Map[String, Team], gd: GameData): GameMapping = {
    val atk = gd.awayTeamKey
    val htk = gd.homeTeamKey
    (teamDict.get(htk), teamDict.get(atk)) match {
      case (None, None) => UnmappedGame(List(htk, atk))
      case (Some(t), None) => UnmappedGame(List(atk))
      case (None, Some(t)) => UnmappedGame(List(htk))
      case (Some(ht), Some(at)) => {
        val game = populateGame(seasonId, updatedBy, gd, ht, at)
        gd.result match {
          case Some(rd) => MappedGameAndResult(game, populateResult(updatedBy, rd))
          case None => MappedGame(game)
        }
      }
    }
  }


  def populateResult(updatedBy: String, r: ResultData): Result = {
    Result(
      id = 0L,
      gameId = 0L,
      homeScore = r.homeScore,
      awayScore = r.awayScore,
      periods = r.periods,
      lockRecord = false,
      updatedAt = LocalDateTime.now(),
      updatedBy = updatedBy)
  }

  def populateGame(seasonId: Long, updatedBy: String, gd: GameData, ht: Team, at: Team): Game = {
    Game(
      id = 0L,
      seasonId = seasonId,
      homeTeamId = ht.id,
      awayTeamId = at.id,
      date = gd.date.toLocalDate,
      datetime = gd.date,
      location = gd.location,
      isNeutralSite = false,
      tourneyKey = gd.tourneyInfo.map(_.region),
      homeTeamSeed = gd.tourneyInfo.map(_.homeTeamSeed),
      awayTeamSeed = gd.tourneyInfo.map(_.awayTeamSeed),
      lockRecord = false,
      updatedAt = LocalDateTime.now(),
      updatedBy = updatedBy
    )
  }

}