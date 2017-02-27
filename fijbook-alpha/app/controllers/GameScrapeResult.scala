package controllers

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO

import scala.concurrent.{ExecutionContext, Future}

case class GameScrapeResult(ids: List[Long] = List.empty[Long], unmappedKeys: List[String] = List.empty[String]) {
  def acc(dao: ScheduleDAO, gm: GameMapping)(implicit ec: ExecutionContext): Future[GameScrapeResult] = {
    gm match {
      case UnmappedGame(ss) => Future.successful(copy(unmappedKeys = unmappedKeys ++ ss))
      case MappedGame(g) => dao.saveGame(g -> None).map(id => copy(ids = id :: ids))
      case MappedGameAndResult(g, r) => dao.saveGame(g -> Some(r)).map(id => copy(ids = id :: ids))
    }
  }
}
