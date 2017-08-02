package controllers

import java.time.LocalDate
import javax.inject.Inject

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.util.IOUtils
import com.fijimf.deepfij.models.S3StaticAsset
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.mohiva.play.silhouette.api.Silhouette
import forms.EditStaticPageForm
import play.api.i18n.I18nSupport
import play.api.mvc.{BaseController, ControllerComponents}
import utils.DefaultEnv

import scala.concurrent.Future

class S3AdminController @Inject()(
                                   val controllerComponents: ControllerComponents,
                                   val teamDao: ScheduleDAO,
                                   val userService: UserService,
                                   val silhouette: Silhouette[DefaultEnv])
  extends BaseController with I18nSupport {


  val s: AmazonS3 = AmazonS3ClientBuilder.standard()
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withEndpointConfiguration(new EndpointConfiguration("s3.amazonaws.com", "us-east-1"))
    .build()

  def listStaticPages = silhouette.SecuredAction.async { implicit rs =>
    val infos: List[S3StaticAsset] = S3StaticAsset.list(s, S3StaticAsset.bucket, S3StaticAsset.staticPageFolder)
    Future.successful(
      Ok(views.html.admin.listStaticPages(rs.identity, infos))
    )
  }

  def createStaticPage = silhouette.SecuredAction.async { implicit rs =>
    val infos: List[S3StaticAsset] = S3StaticAsset.list(s, S3StaticAsset.bucket, S3StaticAsset.staticPageFolder)
    Future.successful(
      Ok(views.html.admin.createStaticPage(rs.identity, EditStaticPageForm.form.fill(EditStaticPageForm.Data("", ""))))
    )
  }

  def editStaticPage(slug: String) = silhouette.SecuredAction.async { implicit rs =>
    val obj = s.getObject(S3StaticAsset.bucket, s"${S3StaticAsset.staticPageFolder}$slug")
    val content = new String(IOUtils.toByteArray(obj.getObjectContent))
    Future.successful(
      Ok(views.html.admin.createStaticPage(rs.identity, EditStaticPageForm.form.fill(EditStaticPageForm.Data(slug, content))))
    )
  }

  def deleteStaticPage(slug: String) = TODO

  def saveStaticPage = silhouette.SecuredAction.async { implicit rs =>
    EditStaticPageForm.form.bindFromRequest.fold(
      form => {
        Future.successful(BadRequest(views.html.admin.createStaticPage(rs.identity, form)))
      },
      data => {
        val version = S3StaticAsset.save(s, S3StaticAsset.bucket, S3StaticAsset.staticPageFolder, data.slug, Map.empty[String, String], data.content)
        val flashMsg = s"Saved static page ${data.slug} $version"
        Future.successful(Redirect(routes.S3AdminController.listStaticPages()).flashing("info" -> flashMsg))
      }
    )
  }

  def listBlogPosts = TODO

  def createBlogPost = TODO

  def editBlogPost = TODO

  def deleteBlogPost = TODO

  def saveBlogPost = TODO

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
