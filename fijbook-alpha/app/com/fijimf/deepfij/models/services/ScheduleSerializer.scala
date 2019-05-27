package com.fijimf.deepfij.models.services

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.sql.Timestamp
import java.time._
import java.util.UUID

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.util.IOUtils
import com.fijimf.deepfij.BuildInfo
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.apache.commons.codec.digest.DigestUtils
import play.api.Logger
import play.api.libs.json.{Json, _}

import scala.concurrent.Future

object ScheduleSerializer {

  val log = Logger(this.getClass)

  val bucket = "deepfij-data"

  import scala.concurrent.ExecutionContext.Implicits.global

  final case class ConfMap(key: String, teams: List[String])

  final case class Scoreboard(date: String, games: List[MappedGame])

  final case class MappedGame(homeTeamKey: String, awayTeamKey: String, date: LocalDate, datetime: LocalDateTime, location: String, isNeutralSite: Boolean, tourneyKey: String, homeTeamSeed: Int, awayTeamSeed: Int, sourceKey: String, result: Map[String, Int])

  final case class MappedSeason(year: Int, confMap: List[ConfMap], scoreboards: List[Scoreboard])

  final case class MappedUniverse(timestamp: LocalDateTime, teams: List[Team], aliases: List[Alias], conferences: List[Conference], quotes: List[Quote], seasons: List[MappedSeason])

  import com.fijimf.deepfij.models._
  implicit val formatsConfMap: Format[ConfMap] = Json.format[ConfMap]
  implicit val formatsMappedGame: Format[MappedGame] = Json.format[MappedGame]
  implicit val formatsScoreboard: Format[Scoreboard] = Json.format[Scoreboard]
  implicit val formatsMappedSeason: Format[MappedSeason] = Json.format[MappedSeason]
  implicit val formatsMappedUniverse: Format[MappedUniverse] = Json.format[MappedUniverse]

  val HOME_SCORE_KEY = "homeScore"
  val AWAY_SCORE_KEY = "awayScore"

  val PERIODS_KEY = "periods"

  def createMappedSeasons(teams: List[Team], conferences: List[Conference], seasons: List[Season], conferenceMaps: List[ConferenceMap], games: List[Game], results: List[Result]): List[MappedSeason] = {
    val teamMap = teams.map(t => t.id -> t).toMap
    val conferenceMap = conferences.map(t => t.id -> t).toMap
    val resultMap = results.map(r => r.gameId -> r).toMap
    seasons.map(s => {
      val confMaps: List[ConfMap] = conferenceMaps.filter(_.seasonId == s.id).groupBy(_.conferenceId).flatMap { case (l: Long, maps: List[ConferenceMap]) =>
        conferenceMap.get(l).map(c => {
          ConfMap(c.key, maps.flatMap(cm => {
            teamMap.get(cm.teamId).map(_.key)
          }))
        })
      }.toList

      val scoreboards = games.filter(g => g.seasonId == s.id).groupBy(_.sourceKey).map { case (src, gs) =>
        val mappedGames: List[MappedGame] = gs.flatMap(g => {
          for {
            homeTeam <- teamMap.get(g.homeTeamId)
            awayTeam <- teamMap.get(g.awayTeamId)
          } yield {
            val mr = resultMap.get(g.id) match {
              case Some(r) => Map(HOME_SCORE_KEY -> r.homeScore, AWAY_SCORE_KEY -> r.awayScore, PERIODS_KEY -> r.periods)
              case None => Map.empty[String, Int]
            }
            MappedGame(homeTeam.key, awayTeam.key, g.date, g.datetime, g.location.getOrElse(""), g.isNeutralSite, g.tourneyKey.getOrElse(""), g.homeTeamSeed.getOrElse(0), g.awayTeamSeed.getOrElse(0), g.sourceKey, mr)
          }
        })
        Scoreboard(src, mappedGames)
      }.toList

      MappedSeason(s.year, confMaps, scoreboards)


    })
  }

  def writeSchedulesToS3(dao: ScheduleDAO): Future[String] = {
    val s3: AmazonS3 = createClient()
    val m = BuildInfo.version

    val k = s"$m-${LocalDateTime.now()}-${UUID.randomUUID().toString}"
    writeSchedulesJson(dao).map(_.getBytes).map(bytes => {
      val inStream = new ByteArrayInputStream(bytes)
      val meta = new ObjectMetadata()
      meta.setContentLength(bytes.length)
      val putReq = new PutObjectRequest(bucket, k, inStream, meta)
      val result: PutObjectResult = s3.putObject(putReq)
      result.getVersionId
    })
  }


  def writeSchedulesJson(dao: ScheduleDAO): Future[String] = {
    val data = for {
      teams <- dao.listTeams
      aliases <- dao.listAliases
      conferences <- dao.listConferences
      quotes <- dao.listQuotes
      seasons <- dao.listSeasons
      conferenceMaps <- dao.listConferenceMaps
      games <- dao.listGames
      results <- dao.listResults
    } yield {
      MappedUniverse(LocalDateTime.now(), teams, aliases, conferences, quotes, createMappedSeasons(teams, conferences, seasons, conferenceMaps, games, results))
    }
    data.map(Json.toJson(_).toString())
  }

  def md5Hash(s:Schedule):String={
    val jsonStr = Json.toJson(createMappedSeasons(s.teams, s.conferences, List(s.season), s.conferenceMap, s.games, s.results)).toString()
    DigestUtils.md5Hex(jsonStr)
  }

  def isDBEmpty(dao: ScheduleDAO): Future[Boolean] = {
    for {
      ts <- dao.listTeams
      cs <- dao.listConferences
      ss <- dao.listSeasons
    } yield {
      ts.isEmpty && cs.isEmpty && ss.isEmpty
    }
  }

  def saveToDb(uni: MappedUniverse, dao: ScheduleDAO, repo: ScheduleRepository, clobberDB: Boolean = false): Future[(Int, Int, Int)] = {
    isDBEmpty(dao).flatMap(b => {
      if (b || clobberDB) {
        saveTheUniverse(uni, dao, repo)
      } else {
        throw new RuntimeException("Database was not empty. Could not load from S3")
      }
    })
  }


  private def saveTheUniverse(uni: MappedUniverse, dao: ScheduleDAO, repo: ScheduleRepository): Future[(Int, Int, Int)]  = {
    log.info("Reloading schedule from snapshot with.")
    repo.dropSchema().flatMap(_ => {
      log.info("Dropped the schema")
      repo.createSchema().flatMap(_ => {
        log.info("Created the schema")
        dao.saveTeams(uni.teams.map(_.copy(id = 0))).flatMap(teams => {
          log.info(s"Saved ${teams.size} teams.")
          dao.saveConferences(uni.conferences.map(_.copy(id = 0L))).flatMap(conferences => {
            log.info(s"Saved ${conferences.size} conferences.")
            dao.saveQuotes(uni.quotes.map(_.copy(id = 0L))).flatMap(quotes => {
              log.info(s"Saved ${quotes.size} quotes.")
              dao.saveAliases(uni.aliases.map(_.copy(id = 0L))).flatMap(aliases => {
                log.info(s"Saved ${aliases.size} aliases.")
                val teamMap: Map[String, Team] = teams.map(t => t.key -> t).toMap
                val confMap: Map[String, Conference] = conferences.map(c => c.key -> c).toMap

                saveSeasons(uni, dao, teamMap, confMap).map(gs=>(teams.size,conferences.size,gs.size))
              }
              )
            })
          })
        })
      })
    }
    )
  }

  def saveSeasons(uni: MappedUniverse, dao: ScheduleDAO, teamMap: Map[String, Team], confMap: Map[String, Conference]): Future[List[Long]] = {
    uni.seasons.foldLeft(Future.successful(List.empty[List[Long]])) { case (fll: Future[List[List[Long]]], ms: MappedSeason) =>
      fll.flatMap(ll => {
        val seas = Season(0L, ms.year)
        dao.saveSeason(seas).flatMap(season => {
          log.info(s"Saving season ${seas.year}.")
          saveSeasonDataToDb(dao, ms, season.id, uni.timestamp, teamMap, confMap).map(gs => {
            gs.map(_._1.id) :: ll
          })
        })
      })
    }.map(_.flatten)
  }

  def saveSeasonDataToDb(dao: ScheduleDAO, ms: MappedSeason, id: Long, ts: LocalDateTime, teamMap: Map[String, Team], confMap: Map[String, Conference]): Future[List[(Game,Option[Result])]] = {
    val conferenceMaps: List[ConferenceMap] = ms.confMap.flatMap(cm => {
      cm.teams.flatMap(tk => {
        for {
          t <- teamMap.get(tk)
          c <- confMap.get(cm.key)
        } yield {
          ConferenceMap(0L, id, c.id, t.id,  ts, "")
        }
      })

    })
    dao.saveConferenceMaps(conferenceMaps).flatMap(_ => {
      Future.sequence(ms.scoreboards.map(s => {
        val gameResults: List[(Game, Option[Result])] = s.games.flatMap(gg => {
          for {
            hh <- teamMap.get(gg.homeTeamKey)
            aa <- teamMap.get(gg.awayTeamKey)
          } yield {
            val g = Game(
              0L,
              id,
              hh.id,
              aa.id,
              gg.date,
              gg.datetime,
              if (gg.location == "") None else Some(gg.location),
              gg.isNeutralSite,
              if (gg.tourneyKey == "") None else Some(gg.tourneyKey),
              if (gg.homeTeamSeed < 1) None else Some(gg.homeTeamSeed),
              if (gg.awayTeamSeed < 1) None else Some(gg.awayTeamSeed),
              s.date,
              ts,
              ""
            )

            val r = for {
              hs <- gg.result.get(HOME_SCORE_KEY)
              as <- gg.result.get(AWAY_SCORE_KEY)
              p <- gg.result.get(PERIODS_KEY)
            } yield {
              Result(0L, 0L, hs, as, p, ts, "")
            }
            (g, r)
          }
        })
        dao.updateGamesWithResults(gameResults)
      })

      ).map(_.flatten)
    })
  }


  def readSchedulesFromS3(key: String, dao: ScheduleDAO, repo: ScheduleRepository): Future[(Int, Int, Int)] = {
    val s3: AmazonS3 = createClient()
    val obj = s3.getObject(bucket, key)
    val bytes = IOUtils.toByteArray(obj.getObjectContent)
    Json.parse(bytes).asOpt[MappedUniverse] match {
      case Some(uni) => saveToDb(uni, dao, repo, clobberDB = true)
      case None => Future.failed(new RuntimeException(s"Failed to parse s3 object for bucket $bucket object $key"))
    }

  }

  def listSaved(): List[(String, Long, LocalDateTime)] = {
    val s3: AmazonS3 = createClient()
    val summaries = s3.listObjects(bucket).getObjectSummaries
    0.until(summaries.size()).map(summaries.get).map(os => (os.getKey, os.getSize, LocalDateTime.ofInstant(os.getLastModified.toInstant, ZoneId.systemDefault())
    )).toList.sortBy(t=> -Timestamp.valueOf(t._3).getTime)
  }

  def readLatestSnapshot(): Option[MappedUniverse] = {
    val s3: AmazonS3 = createClient()
    import scala.collection.JavaConversions._
    val last = s3.listObjects(bucket).getObjectSummaries.maxBy(_.getLastModified.getTime)
    val obj = s3.getObject(bucket, last.getKey)
    Json.parse(IOUtils.toByteArray(obj.getObjectContent)).asOpt[MappedUniverse]
  }

  def deleteSchedulesFromS3(key: String): Future[Option[DeleteObjectsResult]] = {
    Future.successful{
      deleteObjects(bucket, key)
    }
  }

  def deleteObjects(bucket: String, prefix: String): Option[DeleteObjectsResult] = {
    val s3: AmazonS3 = createClient()
    import scala.collection.JavaConversions._
    val list: List[String] = s3.listObjects(bucket, prefix).getObjectSummaries.map(_.getKey()).toList
    if (list.nonEmpty)
      Some(s3.deleteObjects(new DeleteObjectsRequest(bucket).withKeys(list: _*)))
    else
      None
  }


  private def createClient() = {
    val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withEndpointConfiguration(new EndpointConfiguration("s3.amazonaws.com", "us-east-1"))
      .build()
    s3
  }
}
