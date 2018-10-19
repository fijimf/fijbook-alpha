package com.fijimf.deepfij.models

import java.time.LocalDate

final case class GameLine(date:LocalDate, vsAt:String, opp:Team, wl:String, literalScore:String)