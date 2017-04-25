package controllers

import java.time.LocalDateTime
import java.util.UUID

import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future

case class BlogPost
(
  uuid: UUID,
  slug: String,
  title: String,
  subtitle: String,
  author: String,
  date: LocalDateTime,
  sections: List[BlogSection],
  tags: Set[String],
  visible: Boolean,
  releaseDate: Option[LocalDateTime]

)

case class BlogSection(title: String, htmlBody: String)


class BlogController @Inject()(silhouette: Silhouette[DefaultEnv], val messagesApi: MessagesApi) extends Controller with I18nSupport {

  import scala.collection.JavaConversions._

  val logger = Logger(getClass)

  def list = silhouette.SecuredAction.async { implicit request =>
    val s = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withEndpointConfiguration(new EndpointConfiguration("s3.amazonaws.com","us-east-1"))
      .build()
    val listing = s.listObjects("fijimf-2017")
    val string = listing.getObjectSummaries.map(_.getKey).mkString("\n")
    Future.successful(Ok(s"Ta - da\n $string"))
  }

  def create = play.mvc.Results.TODO

  def edit(id: String) = play.mvc.Results.TODO

  def save = play.mvc.Results.TODO

  def delete(id: String) = play.mvc.Results.TODO

  //  def list = silhouette.SecuredAction.async { implicit request =>
  //  }
}
