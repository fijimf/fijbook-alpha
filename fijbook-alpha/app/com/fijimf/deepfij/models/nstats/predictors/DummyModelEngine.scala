package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.{Game, Schedule, XPrediction}

import scala.concurrent.Future

case class DummyModelEngine(state:Option[String]=None) extends ModelEngine[String]{
    override val kernel: Option[String] = state

    override def train(s: List[Schedule], dx: StatValueDAO): Future[ModelEngine[String]] = if (state.isDefined) throw new IllegalStateException() else Future.successful(DummyModelEngine(Some("Trained")))

    override def predict(s: Schedule, ss: StatValueDAO):List[Game] => Future[List[Option[XPrediction]]] = _=>Future.successful(List.empty[Option[XPrediction]])
  }
