package controllers

import com.fijimf.deepfij.auth.services.UserService
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Credentials, PasswordHasherRegistry, PasswordInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import controllers.silhouette.utils.{DefaultEnv, WithProvider}
import forms.silhouette.ChangePasswordForm
import javax.inject.Inject
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * The `Change Password` controller.
  *
  * @param silhouette             The Silhouette stack.
  * @param userService            The user service implementation.
  * @param credentialsProvider    The credentials provider.
  * @param authInfoRepository     The auth info repository.
  * @param passwordHasherRegistry The password hasher registry.
  * @param webJarAssets           The WebJar assets locator.
  */
class ChangePasswordController @Inject() (
                                           val controllerComponents:ControllerComponents,
                                           val dao:ScheduleDAO,

                                           silhouette: Silhouette[DefaultEnv],
                                           userService: UserService,
                                           credentialsProvider: CredentialsProvider,
                                           authInfoRepository: AuthInfoRepository,
                                           passwordHasherRegistry: PasswordHasherRegistry)(implicit ec:ExecutionContext)
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {


  /**
    * Views the `Change Password` page.
    *
    * @return The result to display.
    */
  def view: Action[AnyContent] = silhouette.SecuredAction(WithProvider[DefaultEnv#A](CredentialsProvider.ID)).async { implicit request =>
    for {
      du <- loadDisplayUser(request)
      qw <- getQuoteWrapper(du)
    } yield {
        Ok(views.html.changePassword(du, qw,ChangePasswordForm.form, request.identity))
    }
  }

  /**
    * Changes the password.
    *
    * @return The result to display.
    */
  def submit: Action[AnyContent] = silhouette.SecuredAction(WithProvider[DefaultEnv#A](CredentialsProvider.ID)).async { implicit request =>
    ChangePasswordForm.form.bindFromRequest.fold(
      form => for {
        du <- loadDisplayUser(request)
        qw <- getQuoteWrapper(du)
      } yield {BadRequest(views.html.changePassword(du,qw,form, request.identity))},
      password => {
        val (currentPassword, newPassword) = password
        val credentials = Credentials(request.identity.email.getOrElse(""), currentPassword)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          val passwordInfo = passwordHasherRegistry.current.hash(newPassword)
          authInfoRepository.update[PasswordInfo](loginInfo, passwordInfo).map { _ =>
            Redirect(routes.ChangePasswordController.view()).flashing(FlashUtil.success( Messages("password.changed")))
          }
        }.recover {
          case e: ProviderException =>
            Redirect(routes.ChangePasswordController.view()).flashing(FlashUtil.danger(Messages("current.password.invalid")))
        }
      }
    )
  }
}