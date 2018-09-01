package controllers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ViewUtils {

  def fmt(od:Option[LocalDateTime], pattern:String):String = od match {
    case Some(d)=> fmt(d, pattern)
    case _=> ""
  }

  def fmt(dt:LocalDateTime, pattern:String): String = dt.format(DateTimeFormatter.ofPattern(pattern))

}
