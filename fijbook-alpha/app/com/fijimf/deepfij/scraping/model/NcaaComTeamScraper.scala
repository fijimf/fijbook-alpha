package com.fijimf.deepfij.scraping.model

import play.api.Logger
import cats.implicits._
import scala.xml.Node
trait NcaaComTeamScraper {
  val logger: Logger = Logger.apply(this.getClass)

  def extractNamesAndKeys(schoolList: Option[Node]): Iterator[(String, String)] = {
    for (d <- schoolList.iterator;
         link <- d \\ "a";
         href <- attrValue(link, "href") if href.startsWith("/schools/"))
      yield {
        href.substring(9) -> link.text
      }
  }

  def teamNamesFromStatPage(node: Node):Seq[(String, String)] = {
    val schoolList: Option[Node] = (node \\ "div").find(n => attrMatch(n, "class", "ncaa-stat-category-stats")).flatMap(_.headOption)
    extractNamesAndKeys(schoolList).toSeq
  }

  def schoolName(n: Node): Option[String] = {
    (n \\ "span").find(n => matchClass(n, "school-name")).map(_.text)
  }

  def schoolLogo(n: Node): Option[String] = {
    (n \\ "span").find(n => matchClass(n, "school-logo")).map(_ \\ "img").flatMap(_.headOption).flatMap(nn=>attrValue(nn,"src"))
  }

  def schoolPrimaryColor(n: Node): Option[String] = {
    (n \\ "span").find(n => matchClass(n, "school-logo")).flatMap(nn=>attrValue(nn,"style")).map(_.replaceFirst("border-color:","").replace(";","").trim)
  }
  def schoolOfficialWebsite(n: Node): Option[String] = {
    (n \\ "li").find(n => matchClass(n, "school-social-website")).map(_ \\ "a").flatMap(_.headOption).flatMap(nn=>attrValue(nn,"href"))
  }
  def schoolOfficialTwitter(n: Node): Option[String] = {
    (n \\ "li").find(n => matchClass(n, "school-social-twitter")).map(_ \\ "a").flatMap(_.headOption).flatMap(nn=>attrValue(nn,"href"))
  }
  def schoolOfficialFacebook(n: Node): Option[String] = {
    (n \\ "li").find(n => matchClass(n, "school-social-facebook")).map(_ \\ "a").flatMap(_.headOption).flatMap(nn=>attrValue(nn,"href"))
  }

  def schoolMetaInfo(n:Node):Map[String,String] = {
    val items: Seq[Node] = (n \\ "li").filter(n => attrMatch(n, "class", "school-info"))
    logger.debug(items.mkString("(",", ",")" ))
    items.foldLeft(Map.empty[String, String])((m:Map[String, String], i:Node)=>{
      val key = (i \ "span").text.trim
      val value = i.text.replace(key,"").trim
      m+(key.toLowerCase.replaceAll("\\W","")->value)
    })
  }

  def desaturate(c:String, a:Double):String = {
    val r = Integer.parseInt(c.substring(1,3),16)
    val g = Integer.parseInt(c.substring(3,5),16)
    val b = Integer.parseInt(c.substring(5,7),16)
    "rgba( %d, %d, %d, %f)".format(r,g,b,a)
  }



  def attrValue(n: Node, attr: String): Option[String] = {
    n.attribute(attr).flatMap(_.headOption).map(_.text)
  }

  def attrMatch(n: Node, attr: String, value: String): Boolean = {
    n.attribute(attr) match {
      case Some(nodeStr) => nodeStr.exists(_.text === value)
      case _ => false
    }
  }

  def matchClass(n: Node, value: String): Boolean = {
    n.attribute("class").map(_.text.split(' ').toList) match {
      case Some(vals) => vals.contains(value)
      case _ => false
    }
  }

}
