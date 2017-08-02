package controllers

import java.time.LocalDate
import javax.inject.Inject

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.util.IOUtils
import com.fijimf.deepfij.models.S3StaticAsset
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api.Silhouette
import play.api.Logger
import play.api.mvc.{BaseController, ControllerComponents}
import play.twirl.api.Html
import utils.DefaultEnv

import scala.concurrent.Future

class S3BlockController @Inject()(
                                   val controllerComponents: ControllerComponents,
                                   val teamDao: ScheduleDAO,
                                   val userService: UserService,
                                   val silhouette: Silhouette[DefaultEnv])
  extends BaseController {

  val log = Logger(this.getClass)

  val s = AmazonS3ClientBuilder.standard()
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withEndpointConfiguration(new EndpointConfiguration("s3.amazonaws.com", "us-east-1"))
    .build()

  def blog(folder: String, slug: String) = silhouette.UserAwareAction.async { implicit rs =>
    Future.successful(if (s.doesObjectExist(S3StaticAsset.bucket, s"$folder/$slug")) {
      val obj = s.getObject(S3StaticAsset.bucket, s"$folder/$slug")
      val title = obj.getObjectMetadata.getUserMetadata.get("title")
      val content = new String(IOUtils.toByteArray(obj.getObjectContent))
      Ok(views.html.blog(rs.identity, title, slug, content))
    } else {
      NotFound
    })
  }

  def staticBlock(slug: String) = silhouette.UserAwareAction.async { implicit rs =>
    val key = s"${S3StaticAsset.staticPageFolder}$slug"
    Future.successful(if (s.doesObjectExist(S3StaticAsset.bucket, key)) {
      val obj = s.getObject(S3StaticAsset.bucket, key)
      val content = new String(IOUtils.toByteArray(obj.getObjectContent))
      Ok(views.html.frontPage(rs.identity, LocalDate.now(), content))
    } else {
      NotFound
    })
  }

  def staticPage(slug: String) = silhouette.UserAwareAction.async { implicit rs =>
    val key = s"${S3StaticAsset.staticPageFolder}$slug"
    Future.successful(if (s.doesObjectExist(S3StaticAsset.bucket, key)) {
      val obj = s.getObject(S3StaticAsset.bucket, key)
      val content = new String(IOUtils.toByteArray(obj.getObjectContent))
      Ok(Html(content))
    } else {
      NotFound
    })
  }
}
