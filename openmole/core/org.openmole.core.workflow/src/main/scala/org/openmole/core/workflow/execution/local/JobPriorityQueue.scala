/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.execution.local

import java.util.concurrent.Semaphore
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.task._
import scala.collection.immutable.TreeMap
import scala.collection.mutable.Stack
import org.openmole.core.tools.collection.PriorityQueue

object JobPriorityQueue {

  def apply() =
    PriorityQueue[LocalExecutionJob](
      _.moleJobs.count(mj ⇒ classOf[MoleTask].isAssignableFrom(mj.task.getClass))
    )

}