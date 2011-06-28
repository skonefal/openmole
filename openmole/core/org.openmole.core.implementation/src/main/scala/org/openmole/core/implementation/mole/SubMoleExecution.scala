/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.mole

import org.openmole.core.model.mole.IMoleJobGroup
import org.openmole.core.model.mole.IMoleJobGrouping
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.tools.IContextBuffer
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.IGenericTransition
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.core.implementation.job.Job
import org.openmole.core.implementation.tools.RegistryWithTicket
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.IMoleJob._
import org.openmole.core.model.mole.IGroupingStrategy
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.eventdispatcher.IObjectListenerWithArgs
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.Priority
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

object SubMoleExecution {
  
  def apply(moleExecution: IMoleExecution) = new SubMoleExecution(None, moleExecution)

  def apply(moleExecution: IMoleExecution, parent: ISubMoleExecution) = new SubMoleExecution(Some(parent), moleExecution)
  
}


class SubMoleExecution(val parent: Option[ISubMoleExecution], val moleExecution: IMoleExecution) extends ISubMoleExecution {

  val subMoleExecutionAdapterForMoleJobFinished = new IObjectListenerWithArgs[IMoleJob] {
    override def eventOccured(job: IMoleJob, args: Array[Object]) = {
      val capsule = args(0).asInstanceOf[IGenericCapsule]
      jobFinished(job, capsule)
    }
  }
  
  private var submittedJobs = TreeSet[IMoleJob]()
  private var waiting = List[IJob]()
  private var _nbJobInProgress = 0
  private var _nbJobWaitingInGroup = 0
  private var childs = new HashSet[ISubMoleExecution]
  private val waitingJobs = new HashMap[(IGenericCapsule, IMoleJobGroup), (Job, IGenericCapsule)]
  
  private var canceled = false
  
  val aggregationTransitionRegistry = new RegistryWithTicket[IAggregationTransition, IContextBuffer]
  val transitionRegistry = new RegistryWithTicket[IGenericTransition, IContextBuffer]

  parrentApply(_.addChild(this))

  override def isRoot = !parent.isDefined
  
  override def nbJobInProgess = _nbJobInProgress //_nbJobInProgress
  
  def += (moleJob: IMoleJob) = synchronized {
    submittedJobs += moleJob
    incNbJobInProgress(1)
  }
  
  def -= (moleJob: IMoleJob) = synchronized {
    submittedJobs -= moleJob
    decNbJobInProgress(1)
  }
  
  override def cancel = synchronized {
    submittedJobs.foreach{_.cancel}
    childs.foreach{_.cancel}
    parrentApply(_.removeChild(this))
    canceled = true
  }
  
  override def addChild(submoleExecution: ISubMoleExecution) = synchronized {
    childs += submoleExecution
  }

  override def removeChild(submoleExecution: ISubMoleExecution) = synchronized {
    childs -= submoleExecution
  }
  
  override def incNbJobInProgress(nb: Int) =  {
    synchronized {_nbJobInProgress += nb}
    parrentApply(_.incNbJobInProgress(nb))
  }

  override def decNbJobInProgress(nb: Int) = {
    if(synchronized{_nbJobInProgress -= nb; checkAllJobsWaitingInGroup}) submitJobs
    parrentApply(_.decNbJobInProgress(nb))
  }
  
  override def incNbJobWaitingInGroup(nb: Int) = {
    if(synchronized {_nbJobWaitingInGroup += nb; checkAllJobsWaitingInGroup}) submitJobs
    parrentApply(_.incNbJobWaitingInGroup(nb))
  }

  override def decNbJobWaitingInGroup(nb: Int) = synchronized {
    _nbJobWaitingInGroup -= nb
    parrentApply(_.decNbJobWaitingInGroup(nb))
  }
  
  override def submit(capsule: IGenericCapsule, context: IContext, ticket: ITicket) = synchronized {
    if(!canceled) {
      val moleJob = capsule.toJob(context, moleExecution.nextJobId)

      EventDispatcher.registerForObjectChangedSynchronous(moleJob, Priority.HIGH, subMoleExecutionAdapterForMoleJobFinished, IMoleJob.TransitionPerformed)
      EventDispatcher.registerForObjectChangedSynchronous(moleJob, Priority.HIGH, subMoleExecutionAdapterForMoleJobFinished, IMoleJob.JobFailedOrCanceled)

      this += moleJob
      
      moleExecution.submit(moleJob, capsule, this, ticket)
    }
  }

  override def group(moleJob: IMoleJob, capsule: IGenericCapsule, grouping: Option[IGroupingStrategy]) = synchronized {
    grouping match {
      case Some(strategy) =>
        val category = strategy.group(moleJob.context)

        val key = (capsule, category)
        waitingJobs.getOrElseUpdate(key, (new Job, capsule)) match {
          case (job, capsule) => job += moleJob
        }
        incNbJobWaitingInGroup(1)
      case None =>
        val job = new Job
        job += moleJob
        moleExecution.submitToEnvironment(job, capsule)
    }
  }
    
  private def submitJobs = synchronized {
    waitingJobs.values.foreach {
      case(job, capsule) => 
        moleExecution.submitToEnvironment(job, capsule)
        decNbJobWaitingInGroup(job.moleJobs.size)
    }
    waitingJobs.empty
  }
  
  private def parrentApply(f: ISubMoleExecution => Unit) = 
    parent match {
      case None => 
      case Some(p) => f(p)
    }
    
  private def jobFinished(job: IMoleJob, capsule: IGenericCapsule): Unit = synchronized {       
    this -= job
  }
  
  private def checkAllJobsWaitingInGroup = (nbJobInProgess == _nbJobWaitingInGroup && _nbJobWaitingInGroup > 0)
  
  //private def allWaitingEvent = EventDispatcher.objectChanged(this, ISubMoleExecution.AllJobsWaitingInGroup)
  
}
