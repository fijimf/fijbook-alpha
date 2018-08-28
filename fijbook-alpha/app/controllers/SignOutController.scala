package controllers

import com.mohiva.play.silhouette.api.{LogoutEvent, Silhouette}
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{BaseController, ControllerComponents}

/**
  * The basic application controller.
  *
  * @param silhouette             The Silhouette stack.
  * @param webJarAssets           The webjar assets implementation.
  */
class SignOutController @Inject()(
                                   val controllerComponents: ControllerComponents,

                                   val silhouette: Silhouette[DefaultEnv]

                                   )
  extends BaseController with I18nSupport {

  /**
    * Handles the Sign Out action.
    *
    * @return The result to display.
    */
  def signOut = silhouette.SecuredAction.async { implicit request =>
    val result = Redirect("/")
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }
}