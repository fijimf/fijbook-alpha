package com.fijimf.deepfij.models
import java.time.LocalDate

import scala.concurrent.Future

/**
  * Created by jimfrohnhofer on 2/22/17.
  */
trait GameDAO {

  def listGames: Future[List[Game]]

  def deleteGames(ids: List[Long])

  def upsertGame(game: Game): Future[Long]

  def clearGamesByDate(d: LocalDate): Future[Int]

  def saveGame(gt: (Game, Option[Result])): Future[Long]

  def gamesByDate(d: List[LocalDate]): Future[List[(Game, Option[Result])]]

  def gamesBySource(sourceKey: String): Future[List[(Game, Option[Result])]]
}
