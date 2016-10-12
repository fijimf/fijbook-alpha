package controllers.nav

case class Breadcrumbs(trail:List[Breadcrumb]) {
  def appendChild(display:String, suffix:String): Breadcrumbs = {
    trail.headOption match {
      case Some(b)=>Breadcrumbs(Breadcrumb(display,b.link+suffix)::trail)
      case None => Breadcrumbs(List(Breadcrumb(display,suffix)))
    }
  }
  def appendChild(display:String, suffix:String, icon:String): Breadcrumbs = {
    trail.headOption match {
      case Some(b)=>Breadcrumbs(Breadcrumb(display,b.link+suffix, Some(icon))::trail)
      case None => Breadcrumbs(List(Breadcrumb(display,suffix, Some(icon))))
    }
  }
  def append(display:String, link:String): Breadcrumbs = {
    copy(trail=Breadcrumb(display,link)::trail)
  }
  def append(display:String, link:String, icon:String): Breadcrumbs = {
    copy(trail=Breadcrumb(display,link,Some(icon))::trail)
  }

}

object Breadcrumbs {
  def base=Breadcrumbs(List(Breadcrumb("Deep Fij", "/deepfij", Some("fa-circle-o"))))
}

case class Breadcrumb(display:String, link:String, icon:Option[String]=None)