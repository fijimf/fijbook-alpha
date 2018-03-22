package com.fijimf.deepfij.models.services

import java.io.ByteArrayInputStream
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model._
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import play.api.Application
import play.api.libs.json._

import scala.concurrent.Future

object ScheduleSerializer {

  import scala.concurrent.ExecutionContext.Implicits.global

  case class ConfMap(key: String, teams: List[String])

  case class Scoreboard(date: String, games: List[MappedGame])

  case class MappedGame(homeTeamKey: String, awayTeamKey: String, date: LocalDate, datetime: LocalDateTime, location: String, isNeutralSite: Boolean, tourneyKey: String, homeTeamSeed: Int, awayTeamSeed: Int, sourceKey: String, result: Map[String, Int])

  case class MappedSeason(year: Int, confMap: List[ConfMap], scoreboards: List[Scoreboard])

  implicit val formatsTeam: Format[Team] = Json.format[Team]
  implicit val formatsAlias: Format[Alias] = Json.format[Alias]
  implicit val formatsConference: Format[Conference] = Json.format[Conference]
  implicit val formatsConfMap: Format[ConfMap] = Json.format[ConfMap]
  implicit val formatsMappedGame: Format[MappedGame] = Json.format[MappedGame]
  implicit val formatsScoreboard: Format[Scoreboard] = Json.format[Scoreboard]
  implicit val formatsMappedSeason: Format[MappedSeason] = Json.format[MappedSeason]

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
              case Some(r) => Map("homeScore" -> r.homeScore, "awayScore" -> r.awayScore, "periods" -> r.periods)
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
    val m = System.getProperty("apllication.mode","DEV")
    val bucket = "deepfij-data"
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
      seasons <- dao.listSeasons
      conferenceMaps <- dao.listConferenceMaps
      games <- dao.listGames
      results <- dao.listResults
    } yield {
      val mappedSeasons = createMappedSeasons(teams, conferences, seasons, conferenceMaps, games, results)
      Json.obj(
        "timestamp" -> Json.toJson(LocalDateTime.now().toString),
        "teams" -> Json.toJson(teams),
        "aliases" -> Json.toJson(aliases),
        "conferences" -> Json.toJson(conferences),
        "seasons" -> Json.toJson(mappedSeasons)
      )
    }
    data.map(Json.toJson(_).toString())
  }


  def readSchedulesFromS3() = {}


}
