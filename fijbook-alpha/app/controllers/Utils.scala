package controllers

import play.api.mvc.RequestHeader

object Utils {
  def isAdminRequest(request: RequestHeader) = {
    request.path.startsWith("/deepfij/admin")
  }
}
