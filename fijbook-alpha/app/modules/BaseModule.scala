package modules

import com.fijimf.deepfij.models.services.{AuthTokenService, AuthTokenServiceImpl}
import com.fijimf.deepfij.models.{AuthTokenDAO, AuthTokenDAOImpl}


  import com.google.inject.AbstractModule
  import net.codingwell.scalaguice.ScalaModule

  /**
    * The base Guice module.
    */
  class BaseModule extends AbstractModule with ScalaModule {

    /**
      * Configures the module.
      */
    def configure(): Unit = {
      bind[AuthTokenDAO].to[AuthTokenDAOImpl]
      bind[AuthTokenService].to[AuthTokenServiceImpl]
    }

}