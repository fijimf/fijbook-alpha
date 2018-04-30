package com.fijimf.deepfij.models.dao.schedule

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Result, Season}

import scala.concurrent.Future

trait GameDAO {

  def listGames: Future[List[Game]]

  def deleteGames(ids: List[Long]):Future[Unit]

  def insertGame(game: Game): Future[Game]

  def updateGame(game: Game): Future[Game]

  def updateGames(games: List[Game]): Future[List[Game]]

  def clearGamesByDate(d: LocalDate): Future[Int]

  def saveGame(gt: (Game, Option[Result])): Future[Long]

  def saveGames(gts: List[(Game, Option[Result])]): Future[List[Long]]
  
  def gamesByDate(d: List[LocalDate]): Future[List[(Game, Option[Result])]]

  def gamesBySource(sourceKey: String): Future[List[(Game, Option[Result])]]

  def gamesById(id:Long): Future[Option[(Game, Option[Result])]]

  def teamGames(key:String):Future[List[(Season,Game,Result)]]

}
