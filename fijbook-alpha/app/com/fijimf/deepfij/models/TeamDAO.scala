package com.fijimf.deepfij.models

import com.sun.org.apache.xpath.internal.operations.Gt

import scala.concurrent.Future

/**
  * Give access to the user object.
  */
trait TeamDAO {
  def aliasList: Future[List[Alias]]

  def saveGame(gt: (Game, Option[Result])):Future[Long]

  def saveQuote(q: Qotd): Future[Int]


  def find(key: String): Future[Option[Team]]

  def find(id: Long): Future[Option[Team]]

  def save(team: Team): Future[Int]
  def saveSeason(season: Season): Future[Int]
  def findSeason(id:Long): Future[Option[Season]]

  def unlock(key: String): Future[Int]

  def lock(key: String): Future[Int]

  def list: Future[List[Team]]
}