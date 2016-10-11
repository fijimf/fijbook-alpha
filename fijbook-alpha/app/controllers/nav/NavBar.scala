package controllers.nav

case class NavBar(items:List[NavItem]) {

}

object StandardNavBar {
  def active(key:String="no-active"): NavBar = {
    NavBar(List(
      NavItem("index","index","fa-dashboard","Dashboard",key=="index"),
      NavItem("charts","charts.html", "fa-bar-chart-o", " Charts", key=="charts"),
      NavItem("tables","tables.html", "fa-table", " Tables", key=="tables"),
      NavItem("forms","forms.html", "fa-edit", " Forms", key=="forms"),
      NavItem("elements","bootstrap-elements.html", "fa-desktop", " Bootstrap Elements", key=="elements"),
      NavItem("grid","bootstrap-grid.html", "fa-wrench", " Bootstrap Grid", key=="grid"),
      NavItem("blank","blank-page.html", "fa-file", " Blank Page", key=="blank"),
      NavItem("admin","/deepfij/admin", "fa-file", " Admin", key=="admin")
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
