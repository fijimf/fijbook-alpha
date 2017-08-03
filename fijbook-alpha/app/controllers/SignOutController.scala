package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{LogoutEvent, Silhouette}
import play.api.i18n.I18nSupport
import play.api.mvc.{BaseController, ControllerComponents}
import utils.DefaultEnv

/**
  * The basic application controller.
  *
  * @param silhouette             The Silhouette stack.
  * @param webJarAssets           The webjar assets implementation.
  */
class SignOutController @Inject()(
                                   val controllerComponents: ControllerComponents,

                                   val silhouette: Silhouette[DefaultEnv],

                                   webJarAssets: WebJarAssets)
  extends BaseController with I18nSupport {

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