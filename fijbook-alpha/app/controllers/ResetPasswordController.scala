package controllers

import java.util.UUID

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.{AuthTokenService, UserService}
import com.mohiva.play.silhouette.api
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{PasswordHasherRegistry, PasswordInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import controllers.silhouette.utils.DefaultEnv
import forms.silhouette.ResetPasswordForm
import javax.inject.Inject
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

/**
  * The `Reset Password` controller.
  *
  * @param silhouette             The Silhouette stack.
  * @param userService            The user service implementation.
  * @param authInfoRepository     The auth info repository.
  * @param passwordHasherRegistry The password hasher registry.
  * @param authTokenService       The auth token service implementation.
  * @param webJarAssets           The WebJar assets locator.
  */
class ResetPasswordController @Inject()(
                                         val controllerComponents: ControllerComponents,
                                         val dao: ScheduleDAO,
                                         silhouette: Silhouette[DefaultEnv],
                                         userService: UserService,
                                         authInfoRepository: AuthInfoRepository,
                                         passwordHasherRegistry: PasswordHasherRegistry,
                                         authTokenService: AuthTokenService
                                         )(implicit ec: ExecutionContext)
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {


  def view(token: UUID): Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    for {
      du <- loadDisplayUser(request)
      qw <- getQuoteWrapper(du)
      tok <- authTokenService.validate(token)
    } yield {
      tok match {
        case Some(authToken) => Ok(views.html.resetPassword(du, qw, ResetPasswordForm.form, token))
        case None => Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.reset.link"))
      }
    }
  }

  def submit(token: UUID): Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    authTokenService.validate(token).flatMap {
      case Some(authToken) =>
        ResetPasswordForm.form.bindFromRequest.fold(
          form => for {
            du <- loadDisplayUser(request)
            qw <- getQuoteWrapper(du)
          } yield {
            BadRequest(views.html.resetPassword(du, qw, form, token))
          },
          password => userService.retrieve(authToken.userID).flatMap {
            case Some(user) if user.providerId == CredentialsProvider.ID =>
              val passwordInfo = passwordHasherRegistry.current.hash(password)
              authInfoRepository.update[PasswordInfo](new api.LoginInfo(user.providerId, user.providerKey), passwordInfo).map { _ =>
                Redirect(routes.SignInController.view()).flashing("success" -> Messages("password.reset"))
              }
            case _ => Future.successful(Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.reset.link")))
          }
        )
      case None => Future.successful(Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.reset.link")))
    }
  }
}
