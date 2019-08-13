package controllers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cats.implicits._
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.util.IOUtils
import com.fijimf.deepfij.auth.services.UserService
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{S3BlogMetaData, S3BlogPost, S3StaticAsset}
import com.fijimf.deepfij.staticpages.model.{S3BlogMetaData, S3BlogPost, S3StaticAsset}
import com.mohiva.play.silhouette.api.Silhouette
import controllers.silhouette.utils.DefaultEnv
import forms.{EditBlogPostForm, EditStaticPageForm}
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

class S3AdminController @Inject()(
                                   val controllerComponents: ControllerComponents,
                                   val dao: ScheduleDAO,
                                   val userService: UserService,
                                   val silhouette: Silhouette[DefaultEnv])
  extends BaseController with WithDao with UserEnricher with QuoteEnricher with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val s: AmazonS3 = AmazonS3ClientBuilder.standard()
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withEndpointConfiguration(new EndpointConfiguration("s3.amazonaws.com", "us-east-1"))
    .build()

  def listStaticPages: Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    val l: List[S3StaticAsset] = S3StaticAsset.list(s, S3StaticAsset.bucket, S3StaticAsset.staticPageFolder)
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      Ok(views.html.admin.listStaticPages(du, qw, l))
    }
  }

  def createStaticPage: Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      Ok(views.html.admin.createStaticPage(du, qw, EditStaticPageForm.form.fill(EditStaticPageForm.Data("", ""))))
    }
  }

  def editStaticPage(key: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    val obj = s.getObject(S3StaticAsset.bucket, s"${S3StaticAsset.staticPageFolder}$key")
    val content = new String(IOUtils.toByteArray(obj.getObjectContent))
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      Ok(views.html.admin.createStaticPage(du, qw, EditStaticPageForm.form.fill(EditStaticPageForm.Data(key, content))))
    }
  }

  def deleteStaticPage(key: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    val obj = s.getObject(S3StaticAsset.bucket, s"${S3StaticAsset.staticPageFolder}$key")
    val content = new String(IOUtils.toByteArray(obj.getObjectContent))
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      Ok(views.html.admin.createStaticPage(du, qw, EditStaticPageForm.form.fill(EditStaticPageForm.Data(key, content))))
    }
  }

  def saveStaticPage: Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      EditStaticPageForm.form.bindFromRequest.fold(
        form => {
          BadRequest(views.html.admin.createStaticPage(du, qw, form))
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
          Redirect(routes.S3AdminController.listStaticPages()).flashing("info" -> flashMsg)
        }
      )
    }
  }

  def listBlogPosts: Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    val metas: List[S3BlogMetaData] = S3BlogPost.list(s, S3BlogPost.bucket, S3BlogPost.blogFolder)
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      Ok(views.html.admin.listBlogPosts(du, qw, metas))
    }
  }

  def createBlogPost: Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    val author: Option[String] = rs.identity.firstName.flatMap(f => rs.identity.lastName.map(l => s"$f $l"))
    val data: EditBlogPostForm.Data = EditBlogPostForm.empty.copy(author = author.getOrElse(""))
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      Ok(views.html.admin.createBlogPost(du, qw, EditBlogPostForm.form.fill(data)))
    }
  }

  def editBlogPost(key: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      val metas: List[S3BlogMetaData] = S3BlogPost.list(s, S3BlogPost.bucket, S3BlogPost.blogFolder)
      metas.find(_.key === key) match {
        case Some(b) =>
          val data: EditBlogPostForm.Data = EditBlogPostForm.fromS3BlogPost(S3BlogPost.load(s, b))
          Ok(views.html.admin.createBlogPost(du, qw, EditBlogPostForm.form.fill(data)))
        case None => Redirect(routes.S3AdminController.listBlogPosts()).flashing("error" -> "Page not found")
      }
    }
  }

  def deleteBlogPost(key: String): Action[AnyContent] = TODO

  def saveBlogPost: Action[AnyContent] = silhouette.SecuredAction.async { implicit rs =>
    for {
      du <- loadDisplayUser(rs)
      qw <- getQuoteWrapper(du)
    } yield {
      EditBlogPostForm.form.bindFromRequest.fold(
        form => {
          BadRequest(views.html.admin.createBlogPost(du, qw, form))
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
          Redirect(routes.S3AdminController.listStaticPages()).flashing("info" -> flashMsg)
        }
      )
    }
  }
}
