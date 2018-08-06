package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{FavoriteLink, ScheduleRepository}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait FavoriteLinkDAOImpl extends FavoriteLinkDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def saveFavoriteLink(f: FavoriteLink): Future[FavoriteLink] = db.run(
    (repo.favoriteLinks returning repo.favoriteLinks.map(_.id)).insertOrUpdate(f)
      .flatMap(i => {
        repo.favoriteLinks.filter(ss => ss.id === i.getOrElse(f.id)).result.headOption
      })
  ).map(_.getOrElse(f))

  override def saveFavoriteLinks(fs: List[FavoriteLink]): Future[List[FavoriteLink]] = {
    val ops = fs.map(f =>
      (repo.favoriteLinks returning repo.favoriteLinks.map(_.id))
        .insertOrUpdate(f)
        .flatMap(ii => repo.favoriteLinks.filter(_.id === ii).result)
    )
    db.run(DBIO.sequence(ops).transactionally).map(ls=>{
      ls.zip(fs).map{case (result: Seq[FavoriteLink], link: FavoriteLink) => result.headOption.getOrElse(link)
      }
    })
  }

  override def listFavoriteLinks: Future[List[FavoriteLink]] = db.run(repo.favoriteLinks.to[List].result)

  override def findFavoriteLinksByPage(link: String): Future[List[FavoriteLink]] = db.run(repo.favoriteLinks.filter(_.link === link).to[List].result)

  override def findFavoriteLinksByUser(userId: String): Future[List[FavoriteLink]] = db.run(repo.favoriteLinks.filter(_.userId === userId).to[List].result)

  override def deleteAllFavoriteLinks(): Future[Int] = db.run(repo.favoriteLinks.delete)

  override def deleteUsersFavoriteLinks(userId: String): Future[Int] = db.run(repo.favoriteLinks.filter(_.userId === userId).delete)
}
