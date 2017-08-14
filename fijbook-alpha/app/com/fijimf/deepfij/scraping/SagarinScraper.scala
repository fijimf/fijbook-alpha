package com.fijimf.deepfij.scraping

import scala.util.{Failure, Success, Try}
import scala.xml.Node

trait SagarinScraper {
  val testTeam = "Georgetown"

  def loadPres(node: Node): Seq[Node] = {
    (node \\ "pre").toList
  }

  def loadTeamPre(node: Node): Option[Node] = {
    loadPres(node).find(_.text.contains(testTeam))
  }

  def breakupPre(n: Node): Option[List[String]] = {
    loadTeamPre(n).map(x => {
      val string = x.toString
      string.split("<br clear=\"none\"/>").toList.filter(_.startsWith("<font color=\"#000000\">"))
    })
  }

  def parseLine(t: String, y: Int): Option[SagarinRow] = {
    val s = sanitizeString(t)
    Try {
      val rank = fullTrim(s.substring(23, 27)).toInt
      val name = fullTrim(s.substring(28, 51))
      val value = fullTrim(s.substring(82, 89)).toDouble
      val won = fullTrim(s.substring(119, 123)).toInt
      val lost = fullTrim(s.substring(124, 128)).toInt
      SagarinRow(name, rank, value, won, lost, y, true)
    } match {
      case Success(sr) => Some(sr)
      case Failure(_) => None
    }
  }

  private def sanitizeString(str: String) = {

    val str1 = str.
      replaceAll("\\&amp\\;", "&").
      replaceAll("\\&nbsp\\;?", " ")

    println(s"'$str' -> '$str1'")
    str1
  }

  private def fullTrim(str:String) = {
    str.replaceAll("[\\p{Z}\\s]", " ").trim()
  }

  def results(n:Node, y:Int):Option[List[SagarinRow]] = {
    breakupPre(n).map(_.flatMap(parseLine(_, y)))
  }
}
