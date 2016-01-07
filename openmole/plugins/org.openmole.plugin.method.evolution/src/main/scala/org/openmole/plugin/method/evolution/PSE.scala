///*
// * Copyright (C) 2015 Romain Reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.openmole.plugin.method.evolution
//
//import fr.iscpif.mgo.Individual
//import fr.iscpif.mgo.algorithm.ga
//import fr.iscpif.mgo.fitness._
//import fr.iscpif.mgo.niche._
//import fr.iscpif.mgo.clone._
//
//object PSE {
//
//  def apply(
//    genome: Genome,
//    objectives: Objectives,
//    gridSize: Seq[Double]) = {
//    WorkflowIntegration.DeterministicGA(
//      ga.PSE[Seq[Double], Seq[Int]](grid(gridSize, _.phenotype)),
//      genome,
//      objectives)
//  }
//
//  def apply(
//    genome: Genome,
//    objectives: Objectives,
//    gridSize: Seq[Double],
//    replication: Replication[Seq[FitnessAggregation]]) = {
//    def niche = grid(gridSize, (i: Individual[_, History[Seq[Double]]]) ⇒ StochasticGAIntegration.aggregateSeq(replication.aggregation, i.phenotype.history))
//
//    WorkflowIntegration.StochasticGA(
//      ga.noisyPSE[History[Seq[Double]], Seq[Int]](niche, replication.max, replication.reevaluate),
//      genome,
//      objectives,
//      replication)
//  }
//
//}
//
