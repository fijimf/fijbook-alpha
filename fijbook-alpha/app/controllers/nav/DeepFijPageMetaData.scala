package controllers.nav

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.User

case class DeepFijPageMetaData
(
  title: String,
  heading: String,
  subHeading: String,
  breadcrumbs: Option[Breadcrumbs] = None,
  navBar: NavBar = StandardNavBar.active(),
  user: Option[User] = None,
  pageIcon: PageImage = PageIcon(),
  lineColor: String = "#ddd",
  quoteKey: Option[String] = None,
  datebook: Option[LocalDate] = None,
  now:LocalDateTime = LocalDateTime.now()
) {
  def today=now.toLocalDate
}

sealed trait PageImage
case class PageIcon(imgSrc:String="/assets/images/deepfij.png") extends PageImage
case class DateBook(localDate:LocalDate = LocalDate.now()) extends PageImage