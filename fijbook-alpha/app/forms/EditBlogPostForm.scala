package forms

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.S3BlogPost
import play.api.data.Form
import play.api.data.Forms._

object EditBlogPostForm {

  val form: Form[Data] = Form(
    mapping(
      "key" -> nonEmptyText,
      "date" -> localDate("yyyy-MM-dd"),
      "author" -> nonEmptyText,
      "title" -> nonEmptyText,
      "subTitle" -> text,
      "isPublic" -> boolean,
      "isDeleted" -> boolean,
      "keywords" -> text,
      "content" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  final case class Data(key: String,
                  date: LocalDate,
                  author: String,
                  title: String,
                  subTitle: String,
                  isPublic: Boolean,
                  isDeleted: Boolean,
                  keywords: String,
                  content: String
                 )

  def fromS3BlogPost(b: S3BlogPost): Data = Data(
    key = b.meta.key,
    date = LocalDate.parse(b.meta.date,DateTimeFormatter.ISO_LOCAL_DATE),
    author = b.meta.author,
    title = b.meta.title,
    subTitle = b.meta.subTitle,
    isPublic = b.meta.isPublic,
    isDeleted = b.meta.isDeleted,
    keywords = b.meta.keywords.mkString(" "),
    content = new String(b.content)
  )

  val empty: Data = Data(
    key = "",
    date = LocalDate.now(),
    author = "",
    title = "",
    subTitle = "",
    isPublic = true,
    isDeleted = false,
    keywords = "",
    content = ""
  )
}
