package com.fijimf.deepfij.models.react

import com.fijimf.deepfij.model.auth.User

final case class DisplayUser
(
  user:Option[User],
  isAdmin:Boolean,
  isLoggedIn:Boolean,
  favorites:List[DisplayLink],
  dailyQuotesLiked:List[Int]
){

}
