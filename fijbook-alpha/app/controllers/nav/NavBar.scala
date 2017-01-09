package controllers.nav

import com.fijimf.deepfij.models.User

case class NavBar(items: List[NavItem]) {

}

object StandardNavBar {
  def active(key: String = "no-active", user: Option[User] = None): NavBar = {
    val list = List(
      NavItem("index", "/deepfij/index", "fa-dashboard", " Dashboard", key == "index"),
      NavItem("teams", "/deepfij/teams", "fa-users", " Teams", key == "teams"),
      NavItem("conferences", "/deepfij/conferences", "fa-list", " Conferences", key == "conferences"),
      NavItem("games", "/deepfij/games", "fa-calendar", " Games", key == "games"),
      NavItem("stats", "/deepfij/stats", "fa-area-chart", " Statistics", key == "stats"),
      NavItem("about", "/deepfij/about", "fa-question", " About", key == "about")
    )
    user.flatMap(_.email) match {
      case Some(x) if x == System.getProperty("admin.user", "nope@nope.com") =>
        NavBar(list :+ NavItem("admin", "/deepfij/admin", "fa-wrench", " Admin", key == "admin"))
      case _ => NavBar(list)
    }
  }
}

/*
                        <li>
                            <a href="javascript:;" data-toggle="collapse" data-target="#demo", "fa-arrows-v", " Dropdown <i class="fa fa-fw fa-caret-down", "</a>
                            <ul id="demo" class="collapse">
                                <li>
                                    <a href="#">Dropdown Item</a>
                                </li>
                                <li>
                                    <a href="#">Dropdown Item</a>
                                </li>
                            </ul>
                        </li>

 */
