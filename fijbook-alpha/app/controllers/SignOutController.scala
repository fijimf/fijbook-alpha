package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future

/**
  * The basic application controller.
  *
  * @param messagesApi The Play messages API.
  * @param silhouette The Silhouette stack.
  * @param socialProviderRegistry The social provider registry.
  * @param webJarAssets The webjar assets implementation.
  */
class SignOutController @Inject() (
                                        val messagesApi: MessagesApi,
                                        silhouette: Silhouette[DefaultEnv],
                                        socialProviderRegistry: SocialProviderRegistry,
                                        implicit val webJarAssets: WebJarAssets)
  extends Controller with I18nSupport {

  /**
    * Handles the Sign Out action.
    *
    * @return The result to display.
    */
  def signOut = silhouette.SecuredAction.async { implicit request =>
    val result = Redirect(routes.IndexController.index())
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }
}