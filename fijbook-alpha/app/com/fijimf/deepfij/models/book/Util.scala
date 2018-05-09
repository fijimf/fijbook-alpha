package com.fijimf.deepfij.models.book

import scala.util.{Failure, Success, Try}

object Util {
  def trySequence[B](l: List[Try[B]]): Try[List[B]] = {
    l.reverse.foldLeft[Try[List[B]]](Success(List.empty[B])) { case (bs: Try[List[B]], tryB: Try[B]) => {
      (bs, tryB) match {
        case (Success(list), Success(b)) => Success(b :: list)
        case (Success(list), Failure(thr)) => Failure(thr)
        case (failure, _) => failure
      }
    }}
  }
}
