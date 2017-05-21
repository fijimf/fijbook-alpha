package forms

import java.time.{LocalDate, LocalDateTime}

import play.api.data.Form
import play.api.data.Forms._

object EditBlogPostForm {
  val form = Form(
    mapping(
      "id" -> nonEmptyText,
      "slug" -> nonEmptyText,
      "title" -> nonEmptyText,
      "subtitle" -> nonEmptyText,
      "author" -> nonEmptyText,
      "date" -> localDateTime("MM/dd/yyyy HH:mm"),
      "sections" -> nonEmptyText,
      "tags" -> nonEmptyText,
      "visible" -> boolean,
      "releaseDate" -> localDateTime("MM/dd/yyyy HH:mm")
    )
    (Data.apply)(Data.unapply))

  case class Data
  (
    id: String,
    slug: String,
    title: String,
    subtitle: String,
    author: String,
    date: LocalDateTime,
    sections: String,
    tags: String,
    visible: Boolean,
    releaseDate: LocalDateTime
  )

}
