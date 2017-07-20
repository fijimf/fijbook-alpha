package controllers

import java.time.LocalDate
import javax.inject.Inject

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.util.IOUtils
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api.Silhouette
import play.api.mvc.Controller
import utils.DefaultEnv

import scala.concurrent.Future

class S3BlockController @Inject()(val teamDao: ScheduleDAO, val userService: UserService, val silhouette: Silhouette[DefaultEnv])
  extends Controller {

  val s = AmazonS3ClientBuilder.standard()
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withEndpointConfiguration(new EndpointConfiguration("s3.amazonaws.com", "us-east-1"))
    .build()

  def blog(folder: String, slug: String) = silhouette.UserAwareAction.async { implicit rs =>
    Future.successful(if (s.doesObjectExist("fijimf-2017", s"$folder/$slug.html")) {
      val obj = s.getObject("fijimf-2017", s"$folder/$slug.html")
      val title = obj.getObjectMetadata.getUserMetadata.get("title")
      val content = new String(IOUtils.toByteArray(obj.getObjectContent))
      Ok(views.html.blog(rs.identity, title, slug, content))

    } else {
      Redirect(routes.IndexController.index).flashing("error" -> "Page not found")
    })
  }

  def index(slug: String) = silhouette.UserAwareAction.async { implicit rs =>
    Future.successful(if (s.doesObjectExist("fijimf-2017", s"$slug.html")) {
      val obj = s.getObject("fijimf-2017", s"$slug.html")
      val content = new String(IOUtils.toByteArray(obj.getObjectContent))
      Ok(views.html.frontPage(rs.identity, LocalDate.now(), content))

    } else {
      Redirect(routes.IndexController.index).flashing("error" -> "Page not found")
    })

  }
}
