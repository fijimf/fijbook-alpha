val xs = (-30).to(30)
val span = 5

def fn(x: Double) = {
  if (x > span / 2) {
    math.round(x / span).toDouble
  } else if (x < -span / 2) {
    math.round(x / span).toDouble
  } else {
    0.0
  }
}
xs.foreach(x => {
  println(s"$x=>${fn(x)}")
})








