
package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.services.{AuthTokenService, UserService}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.silhouette.ForgotPasswordForm
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._
import utils.DefaultEnv

import scala.concurrent.Future

/**
  * The `Forgot Password` controller.
  *
  * @param silhouette       The Silhouette stack.
  * @param userService      The user service implementation.
  * @param authTokenService The auth token service implementation.
  * @param mailerClient     The mailer client.
  * @param webJarAssets     The WebJar assets locator.
  */
class ForgotPasswordController @Inject() (
                                           val controllerComponents:ControllerComponents,

                                           silhouette: Silhouette[DefaultEnv],
                                           userService: UserService,
                                           authTokenService: AuthTokenService,
                                           mailerClient: MailerClient,
                                           implicit val webJarAssets: WebJarAssets)
  extends BaseController with I18nSupport {

  def zzz = Action { (rs: Request[AnyContent]) =>
    Ok("OK")
  }
  /**
    * Views the `Forgot Password` page.
    *
    * @return The result to display.
    */
  def view = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.silhouette.forgotPassword(ForgotPasswordForm.form)))
  }

  /**
    * Sends an email with password reset instructions.
    *
    * It sends an email to the given address if it exists in the database. Otherwise we do not show the user
    * a notice for not existing email addresses to prevent the leak of existing email addresses.
    *
    * @return The result to display.
    */
  def submit = silhouette.UnsecuredAction.async { implicit request =>
    ForgotPasswordForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.silhouette.forgotPassword(form))),
      email => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, email)
        val result = Redirect(routes.SignInController.view()).flashing("info" -> Messages("reset.email.sent"))
        userService.retrieve(loginInfo).flatMap {
          case Some(user) if user.email.isDefined =>
            authTokenService.create(user.userID).map { authToken =>
              val url = routes.ResetPasswordController.view(authToken.id).absoluteURL()

              mailerClient.send(Email(
                subject = Messages("email.reset.password.subject"),
                from = Messages("email.from"),
                to = Seq(email),
                bodyText = Some(views.txt.silhouette.emails.resetPassword(user, url).body),
                bodyHtml = Some(views.html.silhouette.emails.resetPassword(user, url).body)
              ))
              result
            }
          case None => Future.successful(result)
        }
      }
    )
  }
}