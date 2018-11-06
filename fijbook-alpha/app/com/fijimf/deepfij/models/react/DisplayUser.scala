package com.fijimf.deepfij.models.react

import com.fijimf.deepfij.models.User

final case class DisplayUser
(
  user:Option[User],
  isAdmin:Boolean,
  isLoggedIn:Boolean,
  favorites:List[DisplayLink],
  dailyQuotesLiked:List[Int]
){

}
