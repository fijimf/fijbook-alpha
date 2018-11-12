package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.{FavoriteLink, Quote}

import scala.concurrent.Future

trait FavoriteLinkDAO {

  def saveFavoriteLink(f: FavoriteLink): Future[FavoriteLink]

  def saveFavoriteLinks(fs: List[FavoriteLink]): Future[List[FavoriteLink]]

  def listFavoriteLinks: Future[List[FavoriteLink]]

  def findFavoriteLinksByPage(link: String): Future[List[FavoriteLink]]

  def findFavoriteLinksByUser(userId:String): Future[List[FavoriteLink]]

  def deleteAllFavoriteLinks(): Future[Int]

  def deleteUsersFavoriteLinks(userId: String): Future[Int]

  def deleteFavoriteLinkForUser(userId:String, url:String):Future[Int]
}
