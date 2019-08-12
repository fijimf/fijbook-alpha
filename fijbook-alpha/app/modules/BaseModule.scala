package modules

import com.fijimf.deepfij.auth.services.{AuthTokenService, AuthTokenServiceImpl}
import com.fijimf.deepfij.auth.services.AuthTokenServiceImpl
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

  class BaseModule extends AbstractModule with ScalaModule {

   override  def configure(): Unit = bind[AuthTokenService].to[AuthTokenServiceImpl]

}
