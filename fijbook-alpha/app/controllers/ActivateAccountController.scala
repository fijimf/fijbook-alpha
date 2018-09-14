package controllers

import java.net.URLDecoder
import java.util.UUID

import com.fijimf.deepfij.models.services.{AuthTokenService, UserService}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

/**
  * The `Activate Account` controller.
  *
  * @param silhouette       The Silhouette stack.
  * @param userService      The user service implementation.
  * @param authTokenService The auth token service implementation.
  * @param mailerClient     The mailer client.
  * @param webJarAssets     The WebJar assets locator.
  */
class ActivateAccountController @Inject()(
                                           val controllerComponents: ControllerComponents,

                                           silhouette: Silhouette[DefaultEnv],
                                           userService: UserService,
                                           authTokenService: AuthTokenService,
                                           mailerClient: MailerClient)(implicit ec: ExecutionContext)
  extends BaseController with I18nSupport {

  def send(email: String) = silhouette.UnsecuredAction.async { implicit request =>
    val decodedEmail = URLDecoder.decode(email, "UTF-8")
    val loginInfo = LoginInfo(CredentialsProvider.ID, decodedEmail)
    val result = Redirect(routes.SignInController.view()).flashing("info" -> Messages("activation.email.sent", decodedEmail))

    userService.retrieve(loginInfo).flatMap {
      case Some(user) if !user.activated =>
        authTokenService.create(user.userID).map { authToken =>
          val url = routes.ActivateAccountController.activate(authToken.id).absoluteURL()

          mailerClient.send(Email(
            subject = Messages("email.activate.account.subject"),
            from = Messages("email.from"),
            to = Seq(decodedEmail),
            bodyText = Some(views.txt.emails.activateAccount(user, url).body),
            bodyHtml = Some(views.html.emails.activateAccount(user, url).body)
          ))
          result
        }
      case None => Future.successful(result)
    }
  }

  def activate(token: UUID) = silhouette.UnsecuredAction.async { implicit request =>
    authTokenService.validate(token).flatMap {
      case Some(authToken) => userService.retrieve(authToken.userID).flatMap {
        case Some(user) if user.loginInfo.providerID == CredentialsProvider.ID =>
          userService.save(user.copy(activated = true)).map { _ =>
            Redirect(routes.SignInController.view()).flashing("success" -> Messages("account.activated"))
          }
        case _ => Future.successful(Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.activation.link")))
      }
      case None => Future.successful(Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.activation.link")))
    }
  }
}