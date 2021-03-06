package controllers

import com.fijimf.deepfij.auth.services.UserService
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Clock, Credentials}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import controllers.silhouette.utils.DefaultEnv
import forms.silhouette.SignInForm
import javax.inject.Inject
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

/**
  * The `Sign In` controller.
  *
  * @param silhouette             The Silhouette stack.
  * @param userService            The user service implementation.
  * @param authInfoRepository     The auth info repository implementation.
  * @param credentialsProvider    The credentials provider.
  * @param configuration          The Play configuration.
  * @param clock                  The clock instance.
  * @param webJarAssets           The webjar assets implementation.
  */
class SignInController @Inject()(
                                  val controllerComponents: ControllerComponents,
                                  val dao: ScheduleDAO,
                                  silhouette: Silhouette[DefaultEnv],
                                  userService: UserService,
                                  authInfoRepository: AuthInfoRepository,
                                  credentialsProvider: CredentialsProvider,
                                  configuration: Configuration,
                                  clock: Clock
                                  )(implicit ec: ExecutionContext)
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with  I18nSupport {


  def view: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    for {
      du<- loadDisplayUser(request)
      qw<-getQuoteWrapper(du)
    } yield {
      Ok(views.html.signIn(du, qw, SignInForm.form))
    }
  }


  def formErrors(form: Form[SignInForm.Data])(implicit req: Request[AnyContent]): Future[Result] = for {
    du <- loadDisplayUser(req)
    qw <- getQuoteWrapper(du)
  } yield {
    BadRequest(views.html.signIn(du, qw, form))
  }

  def success(data:SignInForm.Data)(implicit req: Request[AnyContent]): Future[Result] = {
    val credentials = Credentials(data.email, data.password)
    credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
      val result = Redirect(routes.ReactMainController.index())
      userService.retrieve(loginInfo).flatMap {
        case Some(user) if !user.activated =>
          for {
            du <- loadDisplayUser(req)
            qw <- getQuoteWrapper(du)
          } yield {
            Ok(views.html.activateAccount(du, qw, data.email))
          }
        case Some(user) =>
          val c = configuration.underlying
          silhouette.env.authenticatorService.create(loginInfo).map {
            case authenticator if data.rememberMe =>
              authenticator.copy(
                expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
              )
            case authenticator => authenticator
          }.flatMap { authenticator =>
            silhouette.env.eventBus.publish(LoginEvent(user, req))
            silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
              silhouette.env.authenticatorService.embed(v, result)
            }
          }
        case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
      }
    }.recover {
      case e: ProviderException =>
        Redirect(routes.SignInController.view()).flashing(FlashUtil.danger(Messages("invalid.credentials")))
    }
  }

  def submit: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    SignInForm.form.bindFromRequest.fold(form => formErrors(form), data => success(data))
  }
}
