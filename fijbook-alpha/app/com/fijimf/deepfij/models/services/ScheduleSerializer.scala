package com.fijimf.deepfij.models.services

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import play.api.libs.json.{JsArray, JsValue, Json, Writes}

import scala.concurrent.Future

object ScheduleSerializer {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  case class ConfMap(key:String,teams:List[String])
  case class Scoreboard(date:String, games:List[MappedGame])
  case class MappedGame(homeTeamKey:String, awayTeamKey:String, date: LocalDate, datetime: LocalDateTime, location:String, isNeutralSite: Boolean, tourneyKey: String, homeTeamSeed: Int, awayTeamSeed:Int, sourceKey: String, result:Map[String,Int])
  case class MappedSeason(year:Int, confMap:List[ConfMap],scoreboards:List[Scoreboard])
  implicit val writesTeam: Writes[Team] = new Writes[Team] {
    override def writes(t: Team): JsValue = Json.obj(
      "key" ->  Json.toJson(t.key),
      "name" -> Json.toJson( t.name),
      "longName" -> Json.toJson(t.longName),
      "nickname" ->  Json.toJson(t.nickname),
      "logoLgUrl" ->  Json.toJson( t.logoLgUrl.getOrElse("")),
      "logoSmUrl" ->  Json.toJson(t.logoSmUrl.getOrElse("")),
      "primaryColor" ->  Json.toJson(t.primaryColor.getOrElse("")),
      "secondaryColor" -> Json.toJson( t.secondaryColor.getOrElse("")),
      "officialUrl" ->  Json.toJson(t.officialUrl.getOrElse("")),
      "officialTwitter" ->  Json.toJson(t.officialTwitter.getOrElse("")),
      "officialFacebook" ->  Json.toJson(t.officialFacebook.getOrElse(""))
    )
  }
  
  implicit val writesAlias: Writes[Alias] = new Writes[Alias] {
    override def writes(a:Alias): JsValue = Json.obj(
      "key"-> Json.toJson(a.key),
      "alias"-> Json.toJson(a.alias)
    )
  }

  implicit val writesConference: Writes[Conference] = new Writes[Conference] {
    override def writes(c: Conference): JsValue = Json.obj(
      "key" -> Json.toJson( c.key,
      "name" ->  Json.toJson(c.name,
      "logoLgUrl" ->  Json.toJson(c.logoLgUrl.getOrElse(""))),
      "logoSmUrl" ->  Json.toJson(c.logoSmUrl.getOrElse("")),
      "officialUrl" ->  Json.toJson(c.officialUrl.getOrElse("")),
      "officialTwitter" -> Json.toJson(c.officialTwitter.getOrElse("")),
      "officialFacebook" ->  Json.toJson(c.officialFacebook.getOrElse(""))
    ))
  }

  implicit val writesConfMap: Writes[ConfMap] = new Writes[ConfMap] {
    override def writes(a:ConfMap): JsValue = Json.obj(
      "key"-> Json.toJson(a.key),
      "teams"-> JsArray(a.teams.map(Json.toJson(_)))
    )
  }

  implicit val writesScoreboard: Writes[Scoreboard] = new Writes[Scoreboard] {
    override def writes(a:Scoreboard): JsValue = Json.obj(
      "date"-> Json.toJson(a.date),
      "games"-> JsArray(a.games.map(Json.toJson(_)))
    )
  }

  implicit val writesMappedGame: Writes[MappedGame] = new Writes[MappedGame] {
    override def writes(a:MappedGame): JsValue = Json.obj(
      "homeTeamKey" -> Json.toJson(a.homeTeamKey),
      "awayTeamKey" -> Json.toJson(a.awayTeamKey),
      "date" -> Json.toJson(a.date),
      "datetime" -> Json.toJson(a.datetime),
      "location" -> Json.toJson(a.location),
      "isNeutralSite" -> Json.toJson(a.isNeutralSite),
      "tourneyKey" -> Json.toJson(a.tourneyKey), 
      "homeTeamSeed" ->Json.toJson( a.homeTeamSeed),
      "awayTeamSeed" -> Json.toJson(a.awayTeamSeed), 
      "sourceKey" -> Json.toJson(a.sourceKey ) ,
      "result"->Json.toJson(a.result)
    )
  }

  implicit val writesMappedSeason: Writes[MappedSeason] = new Writes[MappedSeason] {
    override def writes(a:MappedSeason): JsValue = Json.obj(
      "year"->a.year,
      "confMap"->JsArray(a.confMap.map(Json.toJson(_))),
      "scoreboards"->JsArray(a.scoreboards.map(Json.toJson(_)))
    )
  }

  def createMappedSeasons(teams: List[Team], conferences: List[Conference], seasons: List[Season], conferenceMaps: List[ConferenceMap], games: List[Game], results: List[Result]) = {
    val teamMap = teams.map(t=>t.id->t).toMap
    val conferenceMap = conferences.map(t=>t.id->t).toMap
    val resultMap = results.map(r=>r.gameId->r).toMap
    seasons.map(s=>{
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
              case Some(r)=>Map("homeScore"->r.homeScore, "awayScore"->r.awayScore, "periods"->r.periods)
              case None=>Map.empty[String,Int]
            }
            MappedGame(homeTeam.key, awayTeam.key, g.date, g.datetime, g.location.getOrElse(""), g.isNeutralSite, g.tourneyKey.getOrElse(""), g.homeTeamSeed.getOrElse(0), g.awayTeamSeed.getOrElse(0), g.sourceKey,mr)
          }
        })
        Scoreboard(src, mappedGames)
      }.toList
      
      MappedSeason(s.year, confMaps, scoreboards)


    })
  }

  def writeSchedulesToS3(dao:ScheduleDAO): Future[JsValue] ={
    val data=for {
      teams<-dao.listTeams
      aliases<-dao.listAliases
      conferences<-dao.listConferences
      seasons<-dao.listSeasons
      conferenceMaps<-dao.listConferenceMaps
      games<-dao.listGames
      results<-dao.listResults
    } yield {
      val mappedSeasons = createMappedSeasons(teams,conferences,seasons,conferenceMaps,games,results)
      Json.obj(
        "timestamp"->Json.toJson(LocalDateTime.now().toString),
        "teams"->Json.toJson(teams),
        "aliases"->Json.toJson(aliases),
        "conferences"->Json.toJson(conferences),
        "seasons"->Json.toJson(mappedSeasons)
      )
    }
    data.map(Json.toJson(_))
    
  }
  
  def readSchedulesFromS3()={}
  

}
