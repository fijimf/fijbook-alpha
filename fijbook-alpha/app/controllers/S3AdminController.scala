package controllers

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import javax.inject.Inject

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.util.IOUtils
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.UserService
import com.fijimf.deepfij.models.{S3BlogMetaData, S3BlogPost, S3StaticAsset}
import com.mohiva.play.silhouette.api.Silhouette
import forms.{EditBlogPostForm, EditStaticPageForm}
import play.api.i18n.I18nSupport
import play.api.mvc.{BaseController, ControllerComponents}
import controllers.silhouette.utils.DefaultEnv

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
    Future.successful(
      Ok(views.html.admin.createStaticPage(rs.identity, EditStaticPageForm.form.fill(EditStaticPageForm.Data("", ""))))
    )
  }

  def editStaticPage(key: String) = silhouette.SecuredAction.async { implicit rs =>
    val obj = s.getObject(S3StaticAsset.bucket, s"${S3StaticAsset.staticPageFolder}$key")
    val content = new String(IOUtils.toByteArray(obj.getObjectContent))
    Future.successful(
      Ok(views.html.admin.createStaticPage(rs.identity, EditStaticPageForm.form.fill(EditStaticPageForm.Data(key, content))))
    )
  }

  def deleteStaticPage(key: String) = silhouette.SecuredAction.async { implicit rs =>
    val obj = s.getObject(S3StaticAsset.bucket, s"${S3StaticAsset.staticPageFolder}$key")
    val content = new String(IOUtils.toByteArray(obj.getObjectContent))
    Future.successful(
      Ok(views.html.admin.createStaticPage(rs.identity, EditStaticPageForm.form.fill(EditStaticPageForm.Data(key, content))))
    )
  }

  def saveStaticPage = silhouette.SecuredAction.async { implicit rs =>
    EditStaticPageForm.form.bindFromRequest.fold(
      form => {
        Future.successful(BadRequest(views.html.admin.createStaticPage(rs.identity, form)))
      },
      data => {
        val tags = Map("_author" -> rs.identity.email.getOrElse("<ANonymous>"), "_deleted" -> "false")
        val version = S3StaticAsset.save(
          s,
          S3StaticAsset.bucket,
          S3StaticAsset.staticPageFolder,
          data.key,
          tags,
          data.content
        )
        val flashMsg = s"Saved static page ${data.key} $version"
        Future.successful(Redirect(routes.S3AdminController.listStaticPages()).flashing("info" -> flashMsg))
      }
    )
  }

  def listBlogPosts = silhouette.SecuredAction.async { implicit rs =>
    val metas: List[S3BlogMetaData] = S3BlogPost.list(s, S3BlogPost.bucket, S3BlogPost.blogFolder)
    Future.successful(
      Ok(views.html.admin.listBlogPosts(rs.identity, metas))
    )
  }

  def createBlogPost = silhouette.SecuredAction.async { implicit rs =>
    val author: Option[String] = rs.identity.firstName.flatMap(f => rs.identity.lastName.map(l => s"$f $l"))
    val data: EditBlogPostForm.Data = EditBlogPostForm.empty.copy(author = author.getOrElse(("")))
    Future.successful(
      Ok(views.html.admin.createBlogPost(rs.identity, EditBlogPostForm.form.fill(data)))
    )
  }

  def editBlogPost(key: String) = silhouette.SecuredAction.async { implicit rs =>
    Future.successful {
      val metas: List[S3BlogMetaData] = S3BlogPost.list(s, S3BlogPost.bucket, S3BlogPost.blogFolder)
      metas.find(_.key == key) match {
        case Some(b) =>
          val data: EditBlogPostForm.Data = EditBlogPostForm.fromS3BlogPost(S3BlogPost.load(s, b))
          Ok(views.html.admin.createBlogPost(rs.identity, EditBlogPostForm.form.fill(data)))
        case None => Redirect(routes.S3AdminController.listBlogPosts()).flashing("error" -> "Page not found")
      }
    }
  }

  def deleteBlogPost(key: String) = TODO

  def saveBlogPost = silhouette.SecuredAction.async { implicit rs =>
    EditBlogPostForm.form.bindFromRequest.fold(
      form => {
        Future.successful(BadRequest(views.html.admin.createBlogPost(rs.identity, form)))
      },
      data => {
        val meta = S3BlogMetaData(
          key = data.key,
          date = DateTimeFormatter.ISO_LOCAL_DATE.format(data.date),
          author = data.author,
          title = data.title,
          subTitle = data.subTitle,
          isPublic = data.isPublic,
          isDeleted = data.isDeleted,
          keywords = data.keywords.toLowerCase.split("\\s+").map(_.trim).toList,
          lastModified = LocalDateTime.now()
        )
        val post = S3BlogPost(
          meta = meta,
          content = data.content.getBytes
        )
        val version = S3BlogPost.save(s, post)
        val flashMsg = s"Saved static page ${data.key} $version"
        Future.successful(Redirect(routes.S3AdminController.listStaticPages()).flashing("info" -> flashMsg))
      }
    )
  }

  def index(key: String) = silhouette.UserAwareAction.async { implicit rs =>
    Future.successful(if (s.doesObjectExist("fijimf-2017", s"$key.html")) {
      val obj = s.getObject("fijimf-2017", s"$key.html")
      val content = new String(IOUtils.toByteArray(obj.getObjectContent))
      Ok(views.html.frontPage(rs.identity, LocalDate.now(), content))

    } else {
      Redirect(routes.IndexController.index).flashing("error" -> "Page not found")
    })

  }
}
