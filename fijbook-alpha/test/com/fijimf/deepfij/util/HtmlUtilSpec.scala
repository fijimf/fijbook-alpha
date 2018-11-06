package com.fijimf.deepfij.util

import org.scalatest.FlatSpec

import scala.util.{Failure, Success}
import scala.xml.Node

class HtmlUtilSpec extends FlatSpec {
  "HtmlUtil" should
    "load well formed HTML" in {
    val maybeNode: Option[Node] = HtmlUtil.loadOptFromString(
      """
        |<html>
        |<head/>
        |<body><h1>Fridge Rules</h1></body>
        |</html>
      """.stripMargin)
    assert(maybeNode.isDefined)
  }
  it should "load poorly formed HTML" in  {
    assert(HtmlUtil.loadOptFromString(
      """
        |<html>
        |<head/>
        |<body><h1>Fridge Rules</body>
        |</html>
      """.stripMargin).isDefined)
    assert(HtmlUtil.loadOptFromString(
      """
        |<head/>
        |<body><h1>Fridge Rules</body>

      """.stripMargin).isDefined)
    assert(HtmlUtil.loadOptFromString(
      """
        |<body><h1>Fridge Rules</body>
        |</html>
      """.stripMargin).isDefined)
    assert(HtmlUtil.loadOptFromString(
      """
        |<h1>Fridge Rules</h1>
        |
      """.stripMargin).isDefined)
  }
}
