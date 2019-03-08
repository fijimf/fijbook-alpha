package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models._
import controllers.GameMapping

import scala.concurrent.Future

trait ScoreboardDAO {

  def saveGameResult(g:Game,r:Option[Result]):Future[Option[Game]]

  def updateScoreboard(updateData: List[GameMapping], sourceTag: String):Future[(Seq[Long], Seq[Long])]
}