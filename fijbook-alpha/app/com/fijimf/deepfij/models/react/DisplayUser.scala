package com.fijimf.deepfij.models.react

import com.fijimf.deepfij.models.User

case class DisplayUser
(
  user:Option[User],
  isAdmin:Boolean,
  favorites:List[DisplayLink],
  dailyQuotesLiked:List[Int]
){

}
