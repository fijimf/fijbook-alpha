package com.fijimf.deepfij.models.dao.schedule

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Result}

import scala.concurrent.Future

trait GameDAO {

  def listGames: Future[List[Game]]

  def deleteGames(ids: List[Long]):Future[Unit]

  def insertGame(game: Game): Future[Game]

  def updateGame(game: Game): Future[Game]

  def clearGamesByDate(d: LocalDate): Future[Int]

  def saveGame(gt: (Game, Option[Result])): Future[Long]

  def gamesByDate(d: List[LocalDate]): Future[List[(Game, Option[Result])]]

  def gamesBySource(sourceKey: String): Future[List[(Game, Option[Result])]]
}
