package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Alias, FavoriteLink, ScheduleRepository}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait FavoriteLinkDAOImpl extends FavoriteLinkDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def saveFavoriteLink(f: FavoriteLink): Future[FavoriteLink] = db.run(upsert(f))

  override def saveFavoriteLinks(fs: List[FavoriteLink]): Future[List[FavoriteLink]] = {
    db.run(DBIO.sequence(fs.map(upsert)).transactionally)
  }

  private def upsert(x: FavoriteLink) = {
    (repo.favoriteLinks returning repo.favoriteLinks.map(_.id)).insertOrUpdate(x).flatMap {
      case Some(id) => repo.favoriteLinks.filter(_.id === id).result.head
      case None => DBIO.successful(x)
    }
  }


  override def listFavoriteLinks: Future[List[FavoriteLink]] = db.run(repo.favoriteLinks.to[List].result)

  override def findFavoriteLinksByPage(link: String): Future[List[FavoriteLink]] = db.run(repo.favoriteLinks.filter(_.link === link).to[List].result)

  override def findFavoriteLinksByUser(userId: String): Future[List[FavoriteLink]] = db.run(repo.favoriteLinks.filter(_.userId === userId).to[List].result)

  override def deleteAllFavoriteLinks(): Future[Int] = db.run(repo.favoriteLinks.delete)

  override def deleteUsersFavoriteLinks(userId: String): Future[Int] = db.run(repo.favoriteLinks.filter(_.userId === userId).delete)
}
