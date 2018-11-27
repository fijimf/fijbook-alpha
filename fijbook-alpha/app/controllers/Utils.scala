package controllers

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.{Locale, TimeZone}

import play.api.mvc.RequestHeader

import scala.concurrent.duration._

object Utils {

  def hotCacheDuration:Duration=1.minute
  def isAdminRequest(request: RequestHeader) = {
    request.path.startsWith("/deepfij/admin")
  }
  def yyyymmdd(s:String): LocalDate =LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyyMMdd"))

  implicit class LocalDateTimeWrapper(dt: LocalDateTime) {
    lazy val toMillis: Long = toMillisZoned(ZoneId.systemDefault())

    def toMillisZoned(z: ZoneId): Long = dt.atZone(z).toInstant.toEpochMilli

    def isBetween(start: LocalDateTime, end: LocalDateTime, inclusive: Boolean = false): Boolean = {
      if (inclusive) {
        (dt.isEqual(start) || dt.isAfter(start)) && (dt.isEqual(end) || dt.isBefore(end))
      } else {
        dt.isAfter(start) && dt.isBefore(end)
      }
    }

    def fmt(pattern: String): String = dt.format(DateTimeFormatter.ofPattern(pattern))
    def fmtz(pattern: String, zoneId: ZoneId): String = {
      ZonedDateTime.of(dt, zoneId).format(DateTimeFormatter.ofPattern(pattern))
    }

    def fmtzny(pattern:String) = fmtz(pattern, ZoneId.of("America/New_York"))
  }

  implicit class LocalDateWrapper(dt: LocalDate) {
    def fmt(pattern: String): String = dt.format(DateTimeFormatter.ofPattern(pattern))
    def yyyymmdd: String =fmt("yyyyMMdd")
    def isBetween(start: LocalDate, end: LocalDate, inclusive:Boolean): Boolean = {
      if (inclusive) {
        (dt.isEqual(start) || dt.isAfter(start)) && (dt.isEqual(end) || dt.isBefore(end))
      } else {
        dt.isAfter(start) && dt.isBefore(end)
      }
    }
  }
}
