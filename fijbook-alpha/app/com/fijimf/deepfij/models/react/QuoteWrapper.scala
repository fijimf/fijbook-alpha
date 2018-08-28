package com.fijimf.deepfij.models.react

import com.fijimf.deepfij.models.Quote

case class QuoteWrapper(quote: Quote, isLiked: Boolean, canVote: Boolean)