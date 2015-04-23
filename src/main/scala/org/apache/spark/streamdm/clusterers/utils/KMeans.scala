/*
 * Copyright (C) 2015 Holmes Team at HUAWEI Noah's Ark Lab.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.spark.streamdm.classifiers

import org.apache.spark.streamdm.core._

import scala.util.Random

/**
 * The KMeans object computes the k-means clustering given an array of Examples 
 */
object KMeans extends Serializable {

  /* Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   * @param input an Array of Example containing the instances to be clustered
   * @param k the number of clusters (default 10)
   * @param repetitions the number of loops of k-means (default 1000)
   */
  def cluster(input: Array[Example], k: Int = 10, repetitions: Int = 1000)
      : Array[Instance] = {
    //sample k centroids from the input array
    //uses reservoir sampling to sample in one go
    var centroids = input.foldLeft((Array[Instance](),0))((a,e) => {
      if(a._2<k)
        (a._1:+e.in, a._2+1)
      else {
        var dice = Random.nextInt(a._2)
        if(dice<k) a._1(dice) = e.in
        (a._1, a._2+1)
      }
    })._1
    for(i <- 0 until repetitions) {
      //initialize new empty clusters
      var clusters: Array[(Instance,Double)] = 
                      Array.fill[(Instance,Double)](k)((new NullInstance,0.0))
      //find the closest centroid for each Input
      //and assign it to the cluster
      input.foreach(ex => {
        val closest = centroids.foldLeft((0,Double.MaxValue,0))((cl,centr) => {
          val dist = centr.distanceTo(ex.in)
          if(dist<cl._2)
            ((cl._3,dist,cl._3+1))
          else
            ((cl._1,cl._2,cl._3+1))
        })._1
        clusters(closest) = addInstancesToCluster(clusters(closest),
                                                  (ex.in,ex.weight))
      })
      //recompute centroids
      centroids = clusters.foldLeft(Array[Instance]())((a,cl) => {
        val centroid = 
          if(cl._2==0) cl._1
          else cl._1.map(x => x/cl._2)
        a:+centroid
      })
    }
    centroids
  }

  private def addInstancesToCluster(left: (Instance,Double), 
                                    right: (Instance,Double))
                                    : (Instance,Double) = 
    left._1 match {
      case NullInstance() => right
      case _ => (left._1.add(right._1.map(x=>x*right._2)),left._2+right._2)
    }
}
