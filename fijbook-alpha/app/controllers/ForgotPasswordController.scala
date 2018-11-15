
package controllers

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.{AuthTokenService, UserService}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import controllers.silhouette.utils.DefaultEnv
import forms.silhouette.ForgotPasswordForm
import javax.inject.Inject
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * The `Forgot Password` controller.
  *
  * @param silhouette       The Silhouette stack.
  * @param userService      The user service implementation.
  * @param authTokenService The auth token service implementation.
  * @param mailerClient     The mailer client.
  * @param webJarAssets     The WebJar assets locator.
  */
class ForgotPasswordController @Inject()(
                                          val controllerComponents: ControllerComponents,
                                          val dao:ScheduleDAO,
                                          silhouette: Silhouette[DefaultEnv],
                                          userService: UserService,
                                          authTokenService: AuthTokenService,
                                          mailerClient: MailerClient)(implicit ec: ExecutionContext)
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {

  def view: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    for {
      du<- loadDisplayUser(request)
      qw<-getQuoteWrapper(du)
    } yield {Ok(views.html.forgotPassword(du, qw, ForgotPasswordForm.form))}
  }

  /**
    * Sends an email with password reset instructions.
    *
    * It sends an email to the given address if it exists in the database. Otherwise we do not show the user
    * a notice for not existing email addresses to prevent the leak of existing email addresses.
    *
    * @return The result to display.
    */
  def submit: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    ForgotPasswordForm.form.bindFromRequest.fold(
      form => for {
        du<- loadDisplayUser(request)
        qw<-getQuoteWrapper(du)
      } yield {BadRequest(views.html.forgotPassword(du,qw,form))},
      email => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, email)
        val result = Redirect(routes.SignInController.view()).flashing(FlashUtil.info(Messages("reset.email.sent")))
        userService.retrieve(loginInfo).flatMap {
          case Some(user) if user.email.isDefined =>
            authTokenService.create(user.userID).map { authToken =>
              val url = routes.ResetPasswordController.view(authToken.id).absoluteURL()

              mailerClient.send(Email(
                subject = Messages("email.reset.password.subject"),
                from = Messages("email.from"),
                to = Seq(email),
                bodyText = Some(views.txt.emails.resetPassword(user, url).body),
                bodyHtml = Some(views.html.emails.resetPassword(user, url).body)
              ))
              result
            }
          case None => Future.successful(result)
        }
      }
    )
  }
}