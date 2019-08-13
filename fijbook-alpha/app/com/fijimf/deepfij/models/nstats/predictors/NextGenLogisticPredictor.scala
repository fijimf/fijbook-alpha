package com.fijimf.deepfij.models.nstats.predictors

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.time.LocalDate
import java.util.Base64

import cats.implicits._
import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.{Schedule, XPrediction}
import com.fijimf.deepfij.schedule.model.Schedule
import com.fijimf.deepfij.schedule.services.ScheduleSerializer
import play.api.Logger
import smile.classification
import smile.classification.NeuralNetwork.ErrorFunction
import smile.classification._
import smile.regression.NeuralNetwork.ActivationFunction

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

case class NextGenLogisticPredictor(modelId:Long, version:Int, kernel: Option[String] = None) extends Predictor {
  val logger = Logger(this.getClass)

  override def key: String = "spread-based-logistic"

  val logisticRegression: Option[LogisticRegression] = kernel.flatMap(s => deserializeKernel(s))

  def featureExtractor(schedule: Schedule, statDao: StatValueDAO): FeatureExtractor = FeatureExtractor.repeat(BaseLogisticFeatureExtractor(statDao), 5)

  private val capValue = 35

  def categoryExtractor: CategoryExtractor = {
    val v: Double => Option[Double] = SpreadTransformers.trunc(capValue)
    val w: Double => Option[Double] = SpreadTransformers.bucket(5)
    def a(x:Double)=w(x).flatMap(v)

    CategoryExtractor.stitch(
      SpreadCategoryExtractor(SpreadTransformers.handleTies, v),
      SpreadCategoryExtractor(SpreadTransformers.handleTiesWithNoise(.01),v),
      SpreadCategoryExtractor(SpreadTransformers.handleTiesWithNoise(.02),v),
      SpreadCategoryExtractor(SpreadTransformers.handleTiesWithNoise(.03),v),
      SpreadCategoryExtractor(SpreadTransformers.handleTiesWithNoise(.04),v)
    )
  }

  def loadFeaturesAndCategories(schedule: Schedule, statDao: StatValueDAO): Future[List[(Array[Double], Int)]] = {
    val games = schedule.completeGames.filterNot(_._1.date.getMonthValue === 11)
    logger.info(s"For schedule ${schedule.season.year} found ${games.size} games")
    for {
      features <- featureExtractor(schedule, statDao)(games.map(_._1))
      categories <- categoryExtractor(games)
    } yield {
      require (features.size==categories.size)
      val observations = features.sortBy(_._1).zip(categories.sortBy(_._1)).flatMap {
        case (featureMap: (Long,Map[String, Double]), cat: (Long,Option[Double]) )=>
          require (featureMap._1==cat._1)
          for {
            a <- featureMap._2.get("ols.value.diff")
//            b <- featureMap.get("ols.zscore.away")
//            c <- featureMap.get("ols.zscore.home")
//            d <- featureMap.get("mean-margin.zscore.home")
//            e <- featureMap.get("mean-margin.zscore.away")
//            f <- featureMap.get("mean-margin.zscore.diff")
//            g <- featureMap.get("variance-margin.percentile.home")
//            h <- featureMap.get("variance-margin.percentile.away")
//            i <- featureMap.get("variance-margin.percentile.diff")
//            j <- featureMap.get("variance-margin.percentile.sum")
            y <- cat._2.map(_.toInt + capValue)
          } yield {
            (Array(a), y)
          }
      }
      logger.info(s"For schedule ${schedule.season.year} found ${observations.size} observations")
      observations
    }
  }

  def train(ss: List[Schedule], sx: StatValueDAO): Future[Option[String]] = {
    val observations: Future[List[(Array[Double], Int)]] = Future.sequence(ss.map(s => loadFeaturesAndCategories(s, sx))).map(_.flatten)

    observations.map(obs => {
      logger.info(s"Training set has ${obs.size} a elements")
      val (featureVectors, categories) = obs.unzip
      val xs = featureVectors.toArray
      val ys: Array[Int] = categories.toArray
      val hist: Map[Int, Int] = ys.groupBy(i=>i).mapValues(_.length)
      0.to(2*capValue).foreach(i=>println(s"$i   ${hist.get(i)}"))
      ys.foreach(println(_))
      val logisticRegression: LogisticRegression = logit(xs, ys,0.25,1E-5)

      val arr: Array[Double] =Array.fill(2*capValue+1)(0.0)
      obs.foreach(tup => {
        val p = logisticRegression.predict(tup._1,arr)
        println("F= %s  C= %d  C'= %d (%4.2f) p= {%s}".format(tup._1.map("%4.2f".format(_)).mkString(","),tup._2-capValue, p-capValue, expectedScore(arr), arr.map("%5.3f".format(_)).mkString(", ")))
      })
      serializeKernel(logisticRegression)
    })
  }

  def serializeKernel(lr: LogisticRegression): Option[String] = {
    Try {
      val baos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(baos)
      oos.writeObject(lr)
      oos.close()
      Base64.getEncoder.encodeToString(baos.toByteArray)
    }.toOption
  }

  def deserializeKernel(s: String): Option[LogisticRegression] = {
    Try {
      val bais = new ByteArrayInputStream(Base64.getDecoder.decode(s))
      val ois = new ObjectInputStream(bais)
      ois.readObject().asInstanceOf[LogisticRegression]
    }.toOption
  }


  def predict(schedule: Schedule, statDao: StatValueDAO): Future[List[XPrediction]] = {
    val now = LocalDate.now()
    val hash = ScheduleSerializer.md5Hash(schedule)
    val gs = schedule.incompleteGames
    logisticRegression match {
      case None => Future.successful(List.empty[XPrediction])
      case Some(lr) =>
        for {
          features <- featureExtractor(schedule, statDao)(gs)
        } yield {
          gs.zip(features).flatMap { case (g, feat) =>
            feat._2.get("ols.zscore.diff").map(x => {
              val pp = Array.fill[Double](2*capValue+1)(0.0)
              val p: Int = lr.predict(Array(x), pp)
              val spread = expectedScore(pp)
              val homeProb = pp.drop(capValue+1).sum
              val awayProb = pp.take(capValue).sum
              val tieProb = pp.slice(capValue, capValue + 1).sum
              val hp = homeProb + tieProb / 2.0
              val ap =awayProb + tieProb / 2.0

              logger.info(s"For game (${g.id} | ${g.date}), feature $x => probability $p")
              if (hp > ap) {
                XPrediction(0L, g.id, modelId, now, hash, Some(g.homeTeamId), Some(hp), Some(math.max(spread, 0.0)), None)
              } else {
                XPrediction(0L, g.id, modelId, now, hash, Some(g.awayTeamId), Some(ap), Some(math.max(-spread, 0.0)), None)
              }
            })
          }
        }
    }
  }

  private def expectedScore(pp: Array[Double]): Double = {
    require(pp.length === capValue * 2 + 1)
    require(pp.forall(_ >= 0.0))
    require(math.abs(pp.sum-1.0)<0.0001)
    pp.zipWithIndex.map { case (probability: Double, spreadOff: Int) =>
      probability * (spreadOff - capValue)
    }.sum
  }
}
