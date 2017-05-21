package controllers

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future


class BlogController @Inject()(silhouette: Silhouette[DefaultEnv], val messagesApi: MessagesApi) extends Controller with I18nSupport {

  import scala.collection.JavaConversions._

  val logger = Logger(getClass)

  def list = silhouette.SecuredAction.async { implicit request =>
    val s = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withEndpointConfiguration(new EndpointConfiguration("s3.amazonaws.com", "us-east-1"))
      .build()
    val listing = s.listObjects("fijimf-2017")
    val string = listing.getObjectSummaries.map(_.getKey).mkString("\n")
    Future.successful(Ok(s"Ta - da\n $string"))
  }

  def create = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.blog.createBlogPost(request.identity)))
  }

  def edit(id: String) = play.mvc.Results.TODO

  def save = play.mvc.Results.TODO

  def delete(id: String) = play.mvc.Results.TODO

  //  def list = silhouette.SecuredAction.async { implicit request =>
  //  }
}
