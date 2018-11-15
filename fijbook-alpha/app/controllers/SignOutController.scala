package controllers

import com.mohiva.play.silhouette.api.{LogoutEvent, Silhouette}
import controllers.silhouette.utils.DefaultEnv
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

class SignOutController @Inject()(
                                   val controllerComponents: ControllerComponents,
                                   val silhouette: Silhouette[DefaultEnv]
                                   )
  extends BaseController with I18nSupport {

  def signOut: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val result = Redirect("/")
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }
}