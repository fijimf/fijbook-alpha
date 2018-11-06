package forms

import java.util.TimeZone

import org.quartz.CronExpression
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success, Try}

object EditJobForm {

  import play.api.data.format.Formats._
  import play.api.data.format.Formatter

  val cronEntryConstraint: Constraint[String] = Constraint("constraints.cronEntryCheck")({
    plainText =>
      if (CronExpression.isValidExpression(plainText)) {
        Valid
      } else {
        Invalid("Cron expression is not valid")
      }
  })

  val isValidClassConstraint: Constraint[String] = Constraint("constraints.isValidClass")({
    plainText =>
      Try {
        Class.forName(plainText)
      } match {
        case Success(_) => Valid
        case Failure(thr) => Invalid(thr.getMessage)
      }
  })

  val timezoneConstraint: Constraint[String] = Constraint("constraints.timezoneClass")({
    plainText =>
      if (TimeZone.getAvailableIDs.toSet.contains(plainText.trim)) {
        Valid
      } else {
        Invalid("Unknown time zone")
      }
  })

  implicit object FiniteDurationFormatter extends Formatter[FiniteDuration] {
    override val format = Some(("format.finiteDuration", Nil))

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], FiniteDuration] = parsing({
      Duration.create(_) match {
        case f: FiniteDuration => f
        case _ => throw new IllegalArgumentException
      }
    }, "error.finiteDuration", Nil)(key, data)

    override def unbind(key: String, value: FiniteDuration) = Map(key -> value.toString)
  }

  val form: Form[Data] = Form(
    mapping(
      "id" -> default(longNumber, 0L),
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "cron_schedule" -> nonEmptyText.verifying(cronEntryConstraint),
      "timezone" -> nonEmptyText.verifying(timezoneConstraint),
      "actorClass" -> optional(text),
      "message" -> nonEmptyText,
      "timeout" -> of[FiniteDuration],
      "enabled" -> boolean
    )(Data.apply)(Data.unapply)
  )

  final case class Data(id: Long, name: String, description: String, cronSchedule: String, timezone: String, actorClass: Option[String], message:String, timeout: FiniteDuration, isEnabled: Boolean)

}
