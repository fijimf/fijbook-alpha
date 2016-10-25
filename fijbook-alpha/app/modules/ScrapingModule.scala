package modules

import com.fijimf.deepfij.scraping.modules.scraping.ScrapingActor
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ScrapingModule extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
    bindActor[ScrapingActor]("data-load-actor")
  }

}
