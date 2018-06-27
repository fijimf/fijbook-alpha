package com.fijimf.deepfij.util

import java.io.{Reader, StringReader}

import org.xml.sax.InputSource

import scala.util.Try
import scala.util.control.Exception._
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter


object HtmlUtil {

  def loadOptFromReader(r: Reader): Option[Node] = {
    catching(classOf[Exception]).opt {
      new NoBindingFactoryAdapter().loadXML(new InputSource(r), new SAXFactoryImpl().newSAXParser())
    }
  }

  def loadOptFromString(s: String): Option[Node] =  loadOptFromReader(new StringReader(s))

  def loadFromReader(r: Reader): Try[Node] = {
    Try {
      new NoBindingFactoryAdapter().loadXML(new InputSource(r), new SAXFactoryImpl().newSAXParser())
    }
  }

  def loadFromString(s: String): Try[Node] =  loadFromReader(new StringReader(s))

  def attrValue(n: Node, attr: String): Option[String] = {
    n.attribute(attr).flatMap(_.headOption).map(_.text)
  }

  def attrMatch(n: Node, attr: String, value: String): Boolean = {
    n.attribute(attr) match {
      case Some(nodeStr) => nodeStr.exists(_.text == value)
      case _ => false
    }
  }
}