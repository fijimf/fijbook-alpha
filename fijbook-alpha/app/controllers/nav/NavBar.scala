package controllers.nav

case class NavBar(items:List[NavItem]) {

}

object StandardNavBar {
  def active(key:String="no-active"): NavBar = {
    NavBar(List(
      NavItem("index","/deepfij/index","fa-dashboard","Dashboard",key=="index"),
      NavItem("teams","/deepfij/teams", "fa-users", " Teams", key=="teams"),
      NavItem("conferences","/deepfij/conferences", "fa-list", " Conferences", key=="conferences"),
      NavItem("games","forms.html", "fa-calendar", " Games", key=="games"),
      NavItem("stats","bootstrap-elements.html", "fa-area-chart", " Statistics", key=="stats"),
      NavItem("admin","/deepfij/admin", "fa-wrench", " Admin", key=="admin")
    ))
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
