package controllers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import javax.inject.Inject
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.util.IOUtils
import com.fijimf.deepfij.auth.services.UserService
import com.fijimf.deepfij.models.S3StaticAsset
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.mohiva.play.silhouette.api.Silhouette
import forms.EditBlogPostForm
import play.api.Logger
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import play.twirl.api.Html
import controllers.silhouette.utils.DefaultEnv

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

//  def blog(key: String): Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
//    Future.successful{
//      val metas: List[S3BlogMetaData] = S3BlogPost.list(s, S3BlogPost.bucket, S3BlogPost.blogFolder)
//      metas.find(_.key == key) match {
//        case Some(b) =>
//          Ok(views.html.blog(rs.identity,S3BlogPost.load(s, b)))
//        case None => NotFound
//      }
//    }
//  }

//  def blogIndexPage(): Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
//    Future.successful{
//      val metas: List[S3BlogMetaData] = S3BlogPost.list(s, S3BlogPost.bucket, S3BlogPost.blogFolder)
//      metas.filter(bm => !bm.isDeleted && bm.isPublic).sortBy(bm=>LocalDate.parse(bm.date, DateTimeFormatter.ofPattern("yyyy-MM-dd")).toEpochDay)
//      Ok(views.html.blog(rs.identity,S3BlogPost.load(s, metas.head)))
//    }
//  }

//  def staticBlock(key: String) = silhouette.UserAwareAction.async { implicit rs =>
//    val k = s"${S3StaticAsset.staticPageFolder}$key"
//    Future.successful(if (s.doesObjectExist(S3StaticAsset.bucket, k)) {
//      val obj = s.getObject(S3StaticAsset.bucket, k)
//      val content = new String(IOUtils.toByteArray(obj.getObjectContent))
//      if (key=="about"){
//        Ok(views.html.aboutPage(rs.identity, LocalDate.now(), content))
//      } else {
//        Ok(views.html.aboutPage(rs.identity, LocalDate.now(), content))
//      }
//    } else {
//      NotFound
//    })
//  }

  def staticPage(key: String) = silhouette.UserAwareAction.async { implicit rs =>
    val k = s"${S3StaticAsset.staticPageFolder}$key"
    Future.successful(if (s.doesObjectExist(S3StaticAsset.bucket, k)) {
      val obj = s.getObject(S3StaticAsset.bucket, k)
      val content = new String(IOUtils.toByteArray(obj.getObjectContent))
      Ok(Html(content))
    } else {
      NotFound
    })
  }
}
