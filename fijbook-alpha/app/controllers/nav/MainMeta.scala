package controllers.nav

import java.time.LocalDate

import com.fijimf.deepfij.models.User


final case class MainMeta
(
  title: String,
  pageHead: String,
  pageSubHead: String = "",
  breadcrumbs: Option[Breadcrumbs] = None,
  navBar: NavBar = StandardNavBar.active(),
  user: Option[User] = None,
  icon: String = MainMeta.ICON,
  lineColor: String = MainMeta.LINE_COLOR,
  quoteKey: Option[String] = None,
  date: Option[LocalDate] = None
) {
  def withNav(key: String): MainMeta = {
    copy(navBar = StandardNavBar.active(key, user))
  }

  def withCrumbs(bc: Breadcrumbs): MainMeta = {
    copy(breadcrumbs = Some(bc))
  }
}

object MainMeta {
  val ICON = "/assets/images/deepfij.png"
  val LINE_COLOR = "/assets/images/deepfij.png"

  def base(head: String, optUser: Option[User]): MainMeta = MainMeta(
    title = "deepfij",
    pageHead = head,
    user = optUser
  )
}

