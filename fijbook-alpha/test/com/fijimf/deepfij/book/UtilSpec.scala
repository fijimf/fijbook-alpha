package com.fijimf.deepfij.book

import com.fijimf.deepfij.models.book.Util
import org.scalatest.FlatSpec

import scala.util.{Failure, Success, Try}

class UtilSpec extends FlatSpec {

  "A list of Successes" should "be a success of list" in {
    val list:List[Try[Int]] = List(
      Success(1), Success(0), Success(3)
    )
    Util.trySequence(list) match {
      case Success(l)=> assert(l==List(1,0,3))
      case _=> fail()
    }
  }
  "A list of with a failure" should "be a failure" in {
    val list:List[Try[Int]] = List(
      Success(1), Failure(new RuntimeException), Success(3)
    )
    Util.trySequence(list) match {
      case Success(l)=> fail ("should fail ")
      case Failure(thr)=> //OK
    }
  }

}
