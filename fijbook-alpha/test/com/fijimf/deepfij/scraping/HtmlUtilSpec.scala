package com.fijimf.deepfij.scraping

import org.scalatest.FlatSpec

import scala.xml.Node

class HtmlUtilSpec extends FlatSpec {
  "HtmlUtil" should
    "load well formed HTML" in {
    val maybeNode: Option[Node] = HtmlUtil.loadHtmlFromString(
      """
        |<html>
        |<head/>
        |<body><h1>Fridge Rules</h1></body>
        |</html>
      """.stripMargin)
    assert(maybeNode.isDefined)
  }
}
