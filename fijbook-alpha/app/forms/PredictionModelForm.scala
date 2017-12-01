package forms

import java.time.LocalDate

import play.api.data.Form
import play.api.data.Forms._

object PredictionModelForm {

  val form = Form(
    mapping(
      "seasons" -> list(text),
      "features" -> list(text),
      "normalization"-> text,
      "predictFrom"->optional(localDate("yyyy-MM-dd")),
      "predictTo"->optional(localDate("yyyy-MM-dd")),
      "predictTeams"->list(text)
    )(Data.apply)(Data.unapply)
  )

  case class Data
  (
    seasonsIncluded: List[String],
    features: List[String],
    normalization:String,
    predictFrom:Option[LocalDate],
    predictTo:Option[LocalDate],
    predictTeams:List[String]
  )


}