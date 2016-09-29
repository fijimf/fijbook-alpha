package com.fijimf.deepfij.scraping

import org.scalatest.FlatSpec

import scala.io.Source

class NcaaComTeamScraperSpec extends FlatSpec {
  val isGeorgetown = classOf[NcaaComTeamScraperSpec].getResourceAsStream("/test-data/georgetown.html")
  private val georgetown = Source.fromInputStream(isGeorgetown)
  val scraper = new NcaaComTeamScraper {}

  "NcaaComTeamScraper" should "parse the HTML in " in {
    HtmlUtil.loadHtmlFromString(georgetown.mkString) match {
      case Some(n) => assert(n.nonEmpty)
      case None => fail("Failed to parse sample HTML")
    }
  }

  it should "extract an the school logo " in {
    HtmlUtil.loadHtmlFromString(georgetown.mkString).flatMap(n => scraper.schoolLogo(n)) match {
      case Some(logo) => assert(logo=="http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/images/logos/schools/g/georgetown.40.png")
      case None => fail("Failed to parse sample HTML")
    }
  }
  it should "extract an the school name " in {
    HtmlUtil.loadHtmlFromString(georgetown.mkString).flatMap(n => scraper.schoolName(n)) match {
      case Some(logo) => assert(logo=="Georgetown University")
      case None => fail("Failed to identify school name")
    }
  }

  it should "extract an the school facebook " in {
    HtmlUtil.loadHtmlFromString(georgetown.mkString).flatMap(n => scraper.schoolOfficialFacebook(n)) match {
      case Some(logo) => assert(logo=="http://i.turner.ncaa.com/dr/ncaa/ncaa7/release/sites/default/files/images/logos/schools/g/georgetown.40.png")
      case None => fail("Failed to identify facebook")
    }
  }
  it should "extract an the school twitter " in {
    HtmlUtil.loadHtmlFromString(georgetown.mkString).flatMap(n => scraper.schoolOfficialTwitter(n)) match {
      case Some(logo) => assert(logo=="http://www.twitter.com/georgetownhoyas")
      case None => fail("Failed to identify twitter")
    }
  }
  it should "extract an the school website " in {
    HtmlUtil.loadHtmlFromString(georgetown.mkString).flatMap(n => scraper.schoolOfficialWebsite(n)) match {
      case Some(logo) => assert(logo=="http://guhoyas.com")
      case None => fail("Failed to identify website")
    }
  }
  //
  //  it should "pull locations from games" in {
  //    scraper.getGames(Json.parse(scraper.stripCallbackWrapper(nov15))) match {
  //      case Success(js) => assert(js.value.flatMap(scraper.gameLocation) == locations)
  //      case _ => fail()
  //    }
  //  }
  //
  //  it should "pull start date time from games" in {
  //    scraper.getGames(Json.parse(scraper.stripCallbackWrapper(nov15))) match {
  //      case Success(js) => assert(js.value.flatMap(scraper.gameStartTime) == startTimes.map(LocalDateTime.parse(_)))
  //      case _ => fail()
  //    }
  //  }
  //
  //  it should "pull final status" in {
  //    scraper.getGames(Json.parse(scraper.stripCallbackWrapper(nov15))) match {
  //      case Success(js) => assert(js.value.forall(value => scraper.isGameFinal(value).getOrElse(false)))
  //      case _ => fail()
  //    }
  //  }
  //  it should "pull home team " in {
  //    scraper.getGames(Json.parse(scraper.stripCallbackWrapper(nov15))) match {
  //      case Success(js) =>
  //        assert(js.value.map(gg=> scraper.gameHomeTeam(gg))==homeTeamCandidates)
  //      case _ => fail()
  //    }
  //  }
  //  it should "pull away team " in {
  //    scraper.getGames(Json.parse(scraper.stripCallbackWrapper(nov15))) match {
  //      case Success(js) =>
  //        assert(js.value.map(gg=> scraper.gameHomeTeam(gg))==homeTeamCandidates)
  //      case _ => fail()
  //    }
  //  }

  val startTimes: Seq[String] = Seq(
    "2015-11-15T12:00",
    "2015-11-15T13:00",
    "2015-11-15T13:00",
    "2015-11-15T13:30",
    "2015-11-15T14:00",
    "2015-11-15T14:00",
    "2015-11-15T14:00",
    "2015-11-15T14:00",
    "2015-11-15T14:00",
    "2015-11-15T14:00",
    "2015-11-15T14:30",
    "2015-11-15T15:00",
    "2015-11-15T15:00",
    "2015-11-15T15:00",
    "2015-11-15T15:00",
    "2015-11-15T15:00",
    "2015-11-15T15:00",
    "2015-11-15T15:00",
    "2015-11-15T16:00",
    "2015-11-15T16:00",
    "2015-11-15T16:00",
    "2015-11-15T16:00",
    "2015-11-15T16:30",
    "2015-11-15T16:30",
    "2015-11-15T17:00",
    "2015-11-15T17:00",
    "2015-11-15T17:00",
    "2015-11-15T17:00",
    "2015-11-15T17:00",
    "2015-11-15T17:30",
    "2015-11-15T18:00",
    "2015-11-15T18:00",
    "2015-11-15T18:00",
    "2015-11-15T18:30",
    "2015-11-15T19:00",
    "2015-11-15T19:00",
    "2015-11-15T19:00",
    "2015-11-15T20:00",
    "2015-11-15T20:00",
    "2015-11-15T20:00",
    "2015-11-15T21:00",
    "2015-11-15T21:30",
    "2015-11-15T22:00"
  )


  val homeTeamCandidates: Seq[List[String]] = Seq(
    List("ohio-st", "ohio-st", "Ohio State", "ohio-st", "Ohio State", "OHIOST"),
    List("rutgers", "rutgers", "Rutgers", "rutgers", "Rutgers", "RUTGER"),
    List("bucknell", "bucknell", "Bucknell", "bucknell", "Bucknell", "BUCKNL"),
    List("seton-hall", "seton-hall", "Seton Hall", "seton-hall", "Seton Hall", "SETON"),
    List("purdue", "purdue", "Purdue", "purdue", "Purdue", "PURDUE"),
    List("binghamton", "binghamton", "Binghamton", "binghamton", "Binghamton", "BINGHA"),
    List("clemson", "clemson", "Clemson", "clemson", "Clemson", "CLEM"),
    List("lafayette", "lafayette", "Lafayette", "lafayette", "Lafayette", "LAFAYE"),
    List("south-dakota", "south-dakota", "South Dakota", "south-dakota", "South Dakota", "SO DAK"),
    List("cincinnati", "cincinnati", "Cincinnati", "cincinnati", "Cincinnati", "CINCY"),
    List("valparaiso", "valparaiso", "Valparaiso", "valparaiso", "Valparaiso", "VALPO"),
    List("minnesota", "minnesota", "Minnesota", "minnesota", "Minnesota", "MINN"),
    List("florida-st", "florida-st", "Florida State", "florida-st", "Florida State", "FSU"),
    List("illinois", "illinois", "Illinois", "illinois", "Illinois", "ILL"),
    List("jacksonville-st", "jacksonville-st", "Jacksonville St.", "jacksonville-st", "Jacksonville St.", "JAX ST"),
    List("troy", "troy", "Troy", "troy", "Troy", "TROY"),
    List("evansville", "evansville", "Evansville", "evansville", "Evansville", "EVANS"),
    List("denver", "denver", "Denver", "denver", "Denver", "DENVER"),
    List("penn", "penn", "Penn", "penn", "Penn", "PENN"),
    List("high-point", "high-point", "High Point", "high-point", "High Point", "HIGHPT"),
    List("southern-ill", "southern-ill", "Southern Ill.", "southern-ill", "Southern Ill.", "SIU"),
    List("north-carolina", "north-carolina", "North Carolina", "north-carolina", "North Carolina", "UNC"),
    List("western-caro", "western-caro", "Western Caro.", "western-caro", "Western Caro.", "W CAR"),
    List("northern-ill", "northern-ill", "Northern Ill.", "northern-ill", "Northern Ill.", "NIU"),
    List("saint-louis", "saint-louis", "Saint Louis", "saint-louis", "Saint Louis", "ST LOU"),
    List("seattle", "seattle", "Seattle", "seattle", "Seattle", "SEATTL"),
    List("neb-omaha", "neb-omaha", "Omaha", "neb-omaha", "Omaha", "OMAHA"),
    List("iowa", "iowa", "Iowa", "iowa", "Iowa", "IOWA"),
    List("richmond", "richmond", "Richmond", "richmond", "Richmond", "RICH"),
    List("santa-clara", "santa-clara", "Santa Clara", "santa-clara", "Santa Clara", "STCLAR"),
    List("western-ill", "western-ill", "Western Illinois", "western-ill", "Western Illinois", "W ILL"),
    List("missouri", "missouri", "Missouri", "missouri", "Missouri", "MIZZOU"),
    List("north-carolina-st", "north-carolina-st", "NC State", "north-carolina-st", "NC State", "NC ST"),
    List("eastern-wash", "eastern-wash", "Eastern Wash.", "eastern-wash", "Eastern Wash.", "E WASH"),
    List("umkc", "umkc", "UMKC", "umkc", "UMKC", "UMKC"),
    List("nevada", "nevada", "Nevada", "nevada", "Nevada", "NEVADA"),
    List("uc-davis", "uc-davis", "UC Davis", "uc-davis", "UC Davis", "UC DAV"),
    List("wisconsin", "wisconsin", "Wisconsin", "wisconsin", "Wisconsin", "WISC"),
    List("uc-irvine", "uc-irvine", "UC Irvine", "uc-irvine", "UC Irvine", "UC IRV"),
    List("stanford", "stanford", "Stanford", "stanford", "Stanford", "STAN"),
    List("new-mexico-st", "new-mexico-st", "New Mexico St.", "new-mexico-st", "New Mexico St.", "NM ST"),
    List("hawaii", "hawaii", "Hawaii", "hawaii", "Hawaii", "HAWAII"),
    List("ucla", "ucla", "UCLA", "ucla", "UCLA", "UCLA")
  )
  val awayTeamCandidates = Seq(List("mt-st-marys", "mt-st-marys", "Mt. St. Mary's", "mt-st-marys", "Mt. St. Mary's", "MTSTMY"),
    List("howard", "howard", "Howard", "howard", "Howard", "HOWARD"),
    List("wake-forest", "wake-forest", "Wake Forest", "wake-forest", "Wake Forest", "WAKE"),
    List("wagner", "wagner", "Wagner", "wagner", "Wagner", "WAGNER"),
    List("vermont", "vermont", "Vermont", "vermont", "Vermont", "VERMNT"),
    List("army", "army", "Army West Point", "army", "Army West Point", "ARMY"),
    List("utsa", "utsa", "UTSA", "utsa", "UTSA", "UTSA"),
    List("st-peters", "st-peters", "St. Peter's", "st-peters", "St. Peter's", "ST PTR"),
    List("cal-st-northridge", "cal-st-northridge", "CSUN", "cal-st-northridge", "CSUN", "CSUN"),
    List("robert-morris", "robert-morris", "Robert Morris", "robert-morris", "Robert Morris", "ROBMOR"),
    List("iona", "iona", "Iona", "iona", "Iona", "IONA"),
    List("la-monroe", "la-monroe", "La.-Monroe", "la-monroe", "La.-Monroe", "LA MON"),
    List("nicholls-st", "nicholls-st", "Nicholls State", "nicholls-st", "Nicholls State", "NICHST"),
    List("north-dakota-st", "north-dakota-st", "North Dakota St.", "north-dakota-st", "North Dakota St.", "ND ST"),
    List("fort-valley-st", "fort-valley-st", "Fort Valley St.", "fort-valley-st", "Fort Valley St.", "FT VAL"),
    List("reinhardt", "reinhardt", "Reinhardt", "reinhardt", "Reinhardt", "REINHA"),
    List("southeast-mo-st", "southeast-mo-st", "Southeast Mo. St.", "southeast-mo-st", "Southeast Mo. St.", "SEMO"),
    List("lipscomb", "lipscomb", "Lipscomb", "lipscomb", "Lipscomb", "LIPSCO"),
    List("central-conn-st", "central-conn-st", "Cent. Conn. St.", "central-conn-st", "Cent. Conn. St.", "C CONN"),
    List("nc-wesleyan", "nc-wesleyan", "N.C. Wesleyan", "nc-wesleyan", "N.C. Wesleyan", "NC WES"),
    List("florida-am", "florida-am", "Florida A&M", "florida-am", "Florida A&M", "FL A&M"),
    List("fairfield", "fairfield", "Fairfield", "fairfield", "Fairfield", "FAIR"),
    List("unc-asheville", "unc-asheville", "UNC-Asheville", "unc-asheville", "UNC-Asheville", "UNC A"),
    List("wright-st", "wright-st", "Wright State", "wright-st", "Wright State", "WRIGHT"),
    List("hartford", "hartford", "Hartford", "hartford", "Hartford", "HRTFRD"),
    List("sacramento-st", "sacramento-st", "Sacramento St.", "sacramento-st", "Sacramento St.", "SAC ST"),
    List("st-marys-mn", "st-marys-mn", "St. Mary's (Minn.)", "st-marys-mn", "St. Mary's (Minn.)", "STMYMN"),
    List("coppin-st", "coppin-st", "Coppin State", "coppin-st", "Coppin State", "COPPIN"),
    List("stetson", "stetson", "Stetson", "stetson", "Stetson", "STETSN"),
    List("milwaukee", "milwaukee", "Milwaukee", "milwaukee", "Milwaukee", "MILWKE"),
    List("hannibal-la-grange", "hannibal-la-grange", "Hannibal-La Grange", "hannibal-la-grange", "Hannibal-La Grange", "HANNIB"),
    List("md-east-shore", "md-east-shore", "Md.-East. Shore", "md-east-shore", "Md.-East. Shore", "UMES"),
    List("south-ala", "south-ala", "South Alabama", "south-ala", "South Alabama", "S ALA"),
    List("default", "", "", ""),
    List("william-jewell", "william-jewell", "William Jewell", "william-jewell", "William Jewell", "WILJEC"),
    List("montana-st", "montana-st", "Montana State", "montana-st", "Montana State", "MONTST "),
    List("portland", "portland", "Portland", "portland", "Portland", "PORT"),
    List("siena", "siena", "Siena", "siena", "Siena", "SIENA"),
    List("loyola-marymount", "loyola-marymount", "Loyola Marym't", "loyola-marymount", "Loyola Marym't", "LMU"),
    List("charleston-so", "charleston-so", "Charleston So.", "charleston-so", "Charleston So.", "CHARSO"),
    List("new-mexico", "new-mexico", "New Mexico", "new-mexico", "New Mexico", "N MEX"),
    List("coastal-caro", "coastal-caro", "Coastal Caro.", "coastal-caro", "Coastal Caro.", "CO CAR"),
    List("cal-poly", "cal-poly", "Cal Poly", "cal-poly", "Cal Poly", "CALPLY")
  )
  val locations: Seq[String] = Seq(
    "Value City Arena at the Jerome Schottenstein Center, Columbus, OH",
    "Louis Brown Athletic Center, Piscataway, NJ",
    "Sojka Pavilion, Lewisburg, PA",
    "Walsh Gymnasium, South Orange, NJ",
    "Mackey Arena, West Lafayette, IN",
    "Binghamton University Events Center, Binghamton, NY",
    "Littlejohn Coliseum, Clemson, SC",
    "Kirby Sports Center, Easton, PA",
    "NIU Convocation Center, DeKalb, IL",
    "Fifth Third Arena, Cincinnati, OH",
    "Athletics-Recreation Center, Valparaiso, IN",
    "Williams Arena, Minneapolis, MN",
    "Donald L. Tucker Center, Tallahassee, FL",
    "Prairie Capital Convention Center, Springfield, IL",
    "Pete Mathews Coliseum, Jacksonville, AL",
    "Trojan Arena, Troy, AL",
    "Ford Center, Evansville, IN",
    "Leavey Center, Santa Clara, CA",
    "Palestra, Philadelphia, PA",
    "Millis Athletic Convocation Center, High Point, NC",
    "SIU Arena, Carbondale, IL",
    "Dean Smith Center, Chapel Hill, NC",
    "Ramsey Center, Cullowhee, NC",
    "NIU Convocation Center, DeKalb, IL",
    "Chaifetz Arena, St. Louis, MO",
    "KeyArena, Seattle, WA",
    "Baxter Arena, Omaha, NE",
    "Carver-Hawkeye Arena, Iowa City, IA",
    "Robins Center, Richmond, VA",
    "Leavey Center, Santa Clara, CA",
    "Western Hall, Macomb, IL",
    "Mizzou Arena, Columbia, MO",
    "PNC Arena, Raleigh, NC",
    "Reese Court, Cheney, WA",
    "Municipal Auditorium, Kansas City, MO",
    "Stan Sheriff Center, Honolulu, HI",
    "The Pavilion, Davis, CA",
    "Kohl Center, Madison, WI",
    "Bren Events Center, Irvine, CA",
    "Maples Pavilion, Stanford, CA",
    "Pan American Center, Las Cruces, NM",
    "Stan Sheriff Center, Honolulu, HI",
    "Pauley Pavilion, Los Angeles, CA")
}

