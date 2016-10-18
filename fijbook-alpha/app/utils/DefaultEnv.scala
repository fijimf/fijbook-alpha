package utils

import com.fijimf.deepfij.models.User
import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator

/**
  * The default env.
  */
trait DefaultEnv extends Env {
  type I = User
  type A = CookieAuthenticator
}