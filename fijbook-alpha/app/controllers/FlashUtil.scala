package controllers

import org.apache.commons.lang3.StringUtils
import play.api.mvc.Flash

object FlashUtil {
  val CLASS_KEY = "class"

  val BUTTON_KEY = "button"

  val MESSAGE_KEY = "message"

  def primary(msg: String) = Flash(Map(CLASS_KEY -> "alert-primary", BUTTON_KEY->"fa-check-circle", MESSAGE_KEY -> msg))

  def secondary(msg: String) = Flash(Map(CLASS_KEY -> "alert-secondary", BUTTON_KEY->"check-circle", MESSAGE_KEY -> msg))

  def success(msg: String) = Flash(Map(CLASS_KEY -> "alert-success", BUTTON_KEY->"check-circle", MESSAGE_KEY -> msg))

  def danger(msg: String) = Flash(Map(CLASS_KEY -> "alert-danger", BUTTON_KEY->"fa-exclamation-circle", MESSAGE_KEY -> msg))

  def warning(msg: String) = Flash(Map(CLASS_KEY -> "alert-warning", BUTTON_KEY->"fa-exclamation-circle", MESSAGE_KEY -> msg))

  def info(msg: String) = Flash(Map(CLASS_KEY -> "alert-info", BUTTON_KEY->"fa-info-circle", MESSAGE_KEY -> msg))

  def divClass(f: Flash): String = s"alert ${f.get(CLASS_KEY).getOrElse("alert-primary")} alert-dismissable"

  def buttonClass(f: Flash) = s"fa ${f.get(BUTTON_KEY).getOrElse("fa-info-circle")}"

  def message(f: Flash): String = f.get(MESSAGE_KEY).getOrElse("")

  def isDefined(f:Flash):Boolean =StringUtils.isNotBlank(message(f))
}
