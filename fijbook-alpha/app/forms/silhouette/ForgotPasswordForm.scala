package forms.silhouette

import play.api.data.Forms._
import play.api.data._

/**
  * The `Forgot Password` form.
  */
object ForgotPasswordForm {

  /**
    * A play framework form.
    */
  val form: Form[String] = Form(
    "email" -> email
  )
}