package forms

import java.time.LocalDate

import play.api.data.Form
import play.api.data.Forms._

object PredictionModelForm {

  val form: Form[Data] = Form(
    mapping(
      "seasons" -> list(text),
      "excludeMonths"->list(text),
      "features" -> list(text),
      "normalization"-> text,
      "predictFrom"->optional(localDate("yyyy-MM-dd")),
      "predictTo"->optional(localDate("yyyy-MM-dd")),
      "predictTeams"->list(text)
    )(Data.apply)(Data.unapply)
  )

  final case class Data
  (
    seasonsIncluded: List[String],
    excludeMonths: List[String],
    features: List[String],
    normalization:String,
    predictFrom:Option[LocalDate],
    predictTo:Option[LocalDate],
    predictTeams:List[String]
  )


}