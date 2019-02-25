package com.fijimf.deepfij.models.nstats.predictors

import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.{Game, Schedule, XPrediction}

import scala.concurrent.Future

case class DummyModelEngine(state:Option[String]=None, trainedAt:LocalDateTime = LocalDateTime.now()) extends ModelEngine[String]{

    override def train(s: List[Schedule], dx: StatValueDAO): Future[ModelEngine[String]] = if (state.isDefined) throw new IllegalStateException() else Future.successful(DummyModelEngine(Some("Trained")))

    override def predict(s: Schedule, ss: StatValueDAO):List[Game] => Future[List[XPrediction]] = _=>Future.successful(List.empty[XPrediction])
  }
