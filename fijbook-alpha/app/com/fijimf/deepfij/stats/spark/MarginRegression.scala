package com.fijimf.deepfij.stats.spark

import java.sql.Timestamp
import java.time.format.DateTimeFormatter

import com.amazonaws.services.elasticmapreduce.model.StepConfig
import org.apache.spark.ml.feature.{IndexToString, OneHotEncoder, StringIndexer, StringIndexerModel}
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.sql._
import org.apache.spark.sql.expressions._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._


object MarginRegression extends Serializable with SparkStepConfig with DeepFijStats with StatsDbAccess {

  val yyyymmdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  def vec2MapUdf: UserDefinedFunction = udf((v: Vector) => v.toArray.zipWithIndex.map(t => t._2 -> t._1).toMap)

  def mkFilter(season: Int, date: Timestamp): (Int, Timestamp) => Boolean = {
    (s, d) => season == s && d.before(date)
  }

  def mkFilterUdf(season: Int, date: Timestamp): UserDefinedFunction = {
    udf(mkFilter(season, date))
  }


  def createStatistics(session: SparkSession, timestamp: String): DataFrame = {
    val teams = session.read.parquet(s"s3n://deepfij-emr/data/snapshots/$timestamp/teams.parquet")
    val games = session.read.parquet(s"s3n://deepfij-emr/data/snapshots/$timestamp/games.parquet")
    calculate(session, games, teams)
  }

  def teamIndexer(teams: DataFrame, keyCol: String, indexCol: String) = {
   new StringIndexer()
      .setInputCol("key")
      .setOutputCol("key_index").fit(teams).setInputCol(keyCol).setOutputCol(indexCol)
  }

  def calculate(session: SparkSession, games: DataFrame, teams: DataFrame): DataFrame = {
    import session.implicits._
    val results = games.filter("(home_score is not null) and (away_score is not null)")

    val subtractVectorUdf = udf((u: Vector, v: Vector) => VectorUtils.minus(u, v))
    val indexer: StringIndexerModel = new StringIndexer()
      .setInputCol("key")
      .setOutputCol("key_index").fit(teams)
    val indexToString = new IndexToString().setLabels(indexer.labels)
    val homeIndexer = teamIndexer(teams, "home_team","home_ix")
    val awayIndexer = teamIndexer(teams,"away_team","away_ix")
    val g1 = homeIndexer.transform(results).coalesce(1)
    val g2 = awayIndexer.transform(g1)

    val hotEncoder = new OneHotEncoder().setDropLast(false)
    val g3 = hotEncoder.setInputCol("home_ix").setOutputCol("home_vec").transform(g2)
    val g4 = hotEncoder.setInputCol("away_ix").setOutputCol("away_vec").transform(g3)
    val g5 = g4.withColumn("vec", subtractVectorUdf($"home_vec", $"away_vec"))
    val regressionSet = g5.select($"season", $"date", $"vec", ($"home_score" - $"away_score").as("margin").cast(DoubleType))

    val lr = new LinearRegression().setFeaturesCol("vec").setLabelCol("margin")
    val zeroes = Vectors.zeros(teams.count().toInt)
    val seasonDates = loadDates(session, games).collect()
    val subSets: Array[(Int, Timestamp, Vector)] = seasonDates.map((row: Row) => (row.getAs[Int](0), row.getAs[Timestamp](1))).map { case (season: Int, date: Timestamp) =>
      val subset: Dataset[Row] = regressionSet.filter(mkFilterUdf(season, date)($"season", $"date")).coalesce(1)
      if (subset.take(1).nonEmpty)
        (season, date, lr.fit(subset).coefficients)
      else {
        (season, date, zeroes)

      }
    }

    val f1 = subSets.toSeq.toDF("season", "date", "coefficients")
      .select($"season", $"date", explode(vec2MapUdf($"coefficients")).as(Seq("index", "value"))) //.flatMap(r=>{
    indexToString
      .setInputCol("index")
      .setOutputCol("team")
      .transform(f1).select($"season", $"date", $"team", $"value").withColumn("stat", lit("base_ols"))
  }


  override def stepConfig(extraOptions: Map[String, String]): StepConfig = createStepConfig(
    "Calculate margin regression",
    "com.fijimf.deepfij.stats.spark.MarginRegression",
    extraOptions
  )

  override def appName: String = "MarginRegression"
}


  
  