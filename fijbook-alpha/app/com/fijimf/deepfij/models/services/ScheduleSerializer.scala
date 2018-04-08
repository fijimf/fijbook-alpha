package com.fijimf.deepfij.models.services

import java.io.ByteArrayInputStream
import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.util.UUID

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.util.IOUtils
import com.fijimf.deepfij.BuildInfo
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import play.api.libs.json.{Json, _}

import scala.concurrent.Future

object ScheduleSerializer {

  val bucket = "deepfij-data"

  import scala.concurrent.ExecutionContext.Implicits.global

  case class ConfMap(key: String, teams: List[String])

  case class Scoreboard(date: String, games: List[MappedGame])

  case class MappedGame(homeTeamKey: String, awayTeamKey: String, date: LocalDate, datetime: LocalDateTime, location: String, isNeutralSite: Boolean, tourneyKey: String, homeTeamSeed: Int, awayTeamSeed: Int, sourceKey: String, result: Map[String, Int])

  case class MappedSeason(year: Int, confMap: List[ConfMap], scoreboards: List[Scoreboard])

  case class MappedUniverse(timestamp: LocalDateTime, teams: List[Team], aliases: List[Alias], conferences: List[Conference], quotes: List[Quote], seasons: List[MappedSeason])

  implicit val formatsTeam: Format[Team] = Json.format[Team]
  implicit val formatsAlias: Format[Alias] = Json.format[Alias]
  implicit val formatsConference: Format[Conference] = Json.format[Conference]
  implicit val formatsQuotes: Format[Quote] = Json.format[Quote]
  implicit val formatsConfMap: Format[ConfMap] = Json.format[ConfMap]
  implicit val formatsMappedGame: Format[MappedGame] = Json.format[MappedGame]
  implicit val formatsScoreboard: Format[Scoreboard] = Json.format[Scoreboard]
  implicit val formatsMappedSeason: Format[MappedSeason] = Json.format[MappedSeason]
  implicit val formatsMappedUniverse: Format[MappedUniverse] = Json.format[MappedUniverse]

  val HOME_SCORE_KEY = "homeScore"
  val AWAY_SCORE_KEY = "awayScore"

  val PERIODS_KEY = "periods"

  def createMappedSeasons(teams: List[Team], conferences: List[Conference], seasons: List[Season], conferenceMaps: List[ConferenceMap], games: List[Game], results: List[Result]) = {
    val teamMap = teams.map(t => t.id -> t).toMap
    val conferenceMap = conferences.map(t => t.id -> t).toMap
    val resultMap = results.map(r => r.gameId -> r).toMap
    seasons.map(s => {
      val confMaps: List[ConfMap] = conferenceMaps.filter(_.seasonId == s.id).groupBy(_.conferenceId).flatMap { case (l: Long, maps: List[ConferenceMap]) => {
        conferenceMap.get(l).map(c => {
          ConfMap(c.key, maps.flatMap(cm => {
            teamMap.get(cm.teamId).map(_.key)
          }))
        })
      }
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
    val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withEndpointConfiguration(new EndpointConfiguration("s3.amazonaws.com", "us-east-1"))
      .build()
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


  def isDBEmpty(dao: ScheduleDAO): Future[Boolean] = {
    for {
      ts <- dao.listTeams
      cs <- dao.listConferences
      ss <- dao.listSeasons
    } yield {
      ts.isEmpty && cs.isEmpty && ss.isEmpty
    }
  }

  def saveToDb(uni: MappedUniverse, dao: ScheduleDAO, repo: ScheduleRepository, clobberDB: Boolean = false): Future[Unit] = {
    isDBEmpty(dao).map(b => {
      if (b || clobberDB) {
        repo.dropSchema().flatMap(_ => repo.createSchema().flatMap(_ => {
          dao.saveTeams(uni.teams.map(_.copy(id = 0))).flatMap(teams => {
            dao.saveConferences(uni.conferences.map(_.copy(id = 0L))).flatMap(conferences => {
              dao.saveQuotes(uni.quotes.map(_.copy(id = 0L))).flatMap(quotes => {
                dao.saveAliases(uni.aliases.map(_.copy(id = 0L))).flatMap(_ => {
                  val teamMap = teams.map(t => t.key -> t).toMap
                  val confMap = conferences.map(c => c.key -> c).toMap

                  uni.seasons.foldLeft(Future.successful(List.empty[Long])) { case (fll: Future[List[Long]], ms: MappedSeason) => {
                    fll.flatMap(ll => {
                      val seas = Season(0L, ms.year, "", None)
                      dao.saveSeason(seas).flatMap(season => {
                        saveSeasonDataToDb(dao, ms, season.id, uni.timestamp, teamMap, confMap)
                      })
                    })
                  }
                  }
                }
                )
              })
            })
          })
        })
        )
      } else {
        throw new RuntimeException("Database was not empty. Could not load from S3")
      }
    })
  }

  def saveSeasonDataToDb(dao: ScheduleDAO, ms: MappedSeason, id: Long, ts: LocalDateTime, teamMap: Map[String, Team], confMap: Map[String, Conference]): Future[List[Long]] = {
    val conferenceMaps: List[ConferenceMap] = ms.confMap.flatMap(cm => {
      cm.teams.flatMap(tk => {
        for {
          t <- teamMap.get(tk)
          c <- confMap.get(cm.key)
        } yield {
          ConferenceMap(0L, id, c.id, t.id, false, ts, "")
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
        dao.saveGames(gameResults)
      })

      ).map(_.flatten)
    })
  }


  def readSchedulesFromS3(key: String, dao: ScheduleDAO, repo: ScheduleRepository): Future[Unit] = {
    val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withEndpointConfiguration(new EndpointConfiguration("s3.amazonaws.com", "us-east-1"))
      .build()


    val obj = s3.getObject(bucket, key)
    Json.parse(IOUtils.toByteArray(obj.getObjectContent)).asOpt[MappedUniverse] match {
      case Some(uni) => saveToDb(uni, dao, repo, clobberDB = true)
      case None => Future {
        println("If failed")
      }
    }

  }

  def listSaved(): List[(String, Long, LocalDateTime)] = {
    val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withEndpointConfiguration(new EndpointConfiguration("s3.amazonaws.com", "us-east-1"))
      .build()
    val summaries = s3.listObjects(bucket).getObjectSummaries
    0.until(summaries.size()).map(summaries.get(_)).map(os => (os.getKey, os.getSize, LocalDateTime.ofInstant(os.getLastModified.toInstant, ZoneId.systemDefault())
    )).toList
  }

  def readLatestSnapshot(): Option[MappedUniverse] = {
    val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withEndpointConfiguration(new EndpointConfiguration("s3.amazonaws.com", "us-east-1"))
      .build()
    import scala.collection.JavaConversions._
    val last = s3.listObjects(bucket).getObjectSummaries.maxBy(_.getLastModified.getTime)
    val obj = s3.getObject(bucket, last.getKey)
    Json.parse(IOUtils.toByteArray(obj.getObjectContent)).asOpt[MappedUniverse]
  }

  def deleteObjects(bucket: String, prefix: String): DeleteObjectsResult = {
    val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withEndpointConfiguration(new EndpointConfiguration("s3.amazonaws.com", "us-east-1"))
      .build()
    import scala.collection.JavaConversions._
    val list: List[String] = s3.listObjects(bucket, prefix).getObjectSummaries.map(_.getKey()).toList
    s3.deleteObjects(new DeleteObjectsRequest(bucket).withKeys(list: _*))
  }


}