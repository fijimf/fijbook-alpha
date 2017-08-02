package controllers

import javax.inject.Inject

import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.providers._
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{BaseController, ControllerComponents}
import utils.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

/**
  * The social auth controller.
  *
  * @param silhouette             The Silhouette stack.
  * @param userService            The user service implementation.
  * @param authInfoRepository     The auth info service implementation.
  * @param socialProviderRegistry The social provider registry.
  * @param webJarAssets           The webjar assets implementation.
  */
class SocialAuthController @Inject()(
                                      val controllerComponents: ControllerComponents,

                                      silhouette: Silhouette[DefaultEnv],
                                      userService: UserService,
                                      authInfoRepository: AuthInfoRepository,
                                      socialProviderRegistry: SocialProviderRegistry,
                                      implicit val webJarAssets: WebJarAssets)(implicit ec: ExecutionContext)
  extends BaseController with I18nSupport with Logger {

  /**
    * Authenticates a user against a social provider.
    *
    * @param provider The ID of the provider to authenticate against.
    * @return The result to display.
    */
  def authenticate(provider: String) = Action.async { implicit request =>
    (socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => for {
            profile <- p.retrieveProfile(authInfo)
            user <- userService.save(profile)
            authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
            authenticator <- silhouette.env.authenticatorService.create(profile.loginInfo)
            value <- silhouette.env.authenticatorService.init(authenticator)
            result <- silhouette.env.authenticatorService.embed(value, Redirect(routes.IndexController.index()))
          } yield {
            silhouette.env.eventBus.publish(LoginEvent(user, request))
            result
          }
        }
      case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
    }).recover {
      case e: ProviderException =>
        logger.error("Unexpected provider error", e)
        Redirect(routes.SignInController.view()).flashing("error" -> Messages("could.not.authenticate"))
    }
  }
}