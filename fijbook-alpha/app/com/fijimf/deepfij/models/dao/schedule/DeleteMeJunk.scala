package com.fijimf.deepfij.models.dao.schedule

import scala.concurrent.Future

object DeleteMeJunk {

  def main(args: Array[String]): Unit = {
    val f: Future[Int] = Future.successful {
      1 / 0
    }

  }
}
