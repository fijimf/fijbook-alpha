package com.fijimf.deepfij.stats.spark

import org.apache.spark.ml.linalg.{DenseVector, SparseVector, Vector}

object VectorUtils {

  def plus(u: Vector, v: Vector): Vector = pairwise(u, v, _ + _)

  def minus(u: Vector, v: Vector): Vector = pairwise(u, v, _ - _)

  def pairwise(u: Vector, v: Vector, op: (Double, Double) => Double): Vector = {
    require(u.size == v.size)
    (u, v) match {
      case (u1: SparseVector, v1: SparseVector) =>
        val indices = u1.indices.toSet.union(v1.indices.toSet).toList.sorted
        val values = indices.map(i => op(u1(i), v1(i)))
        new SparseVector(u1.size, indices.toArray, values.toArray)
      case (u1: Vector, v1: Vector) => {
        new DenseVector(u1.toArray.zip(v1.toArray).map(t => op(t._1, t._2)))
      }
    }
  }


}
