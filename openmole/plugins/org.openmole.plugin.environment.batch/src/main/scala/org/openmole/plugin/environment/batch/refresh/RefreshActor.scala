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

package org.openmole.plugin.environment.batch.refresh

import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.plugin.environment.batch.environment.BatchEnvironment
import org.openmole.plugin.environment.batch.environment.ResubmitException
import org.openmole.tool.logger.Logger

object RefreshActor extends Logger {

  def receive(refresh: Refresh)(implicit services: BatchEnvironment.Services) = {
    import services._

    val Refresh(job, sj, bj, delay, updateErrorsInARow) = refresh
    if (!job.state.isFinal) {
      try bj.jobService.tryWithToken {
        case Some(t) ⇒
          val oldState = job.state
          job.state = bj.updateState(t)
          if (job.state == DONE) JobManager ! GetResult(job, sj, bj.resultPath)
          else if (!job.state.isFinal) {
            val newDelay =
              if (oldState == job.state)
                (delay + job.environment.updateInterval.incrementUpdateInterval) min job.environment.updateInterval.maxUpdateInterval
              else job.environment.updateInterval.minUpdateInterval
            JobManager ! Delay(Refresh(job, sj, bj, newDelay, 0), newDelay)
          }
          else JobManager ! Kill(job)
        case None ⇒ JobManager ! Delay(Refresh(job, sj, bj, delay, updateErrorsInARow), BatchEnvironment.getTokenInterval)
      } catch {
        case _: ResubmitException ⇒
          JobManager ! Resubmit(job, sj.storage)
        case e: Throwable ⇒
          if (updateErrorsInARow >= preference(BatchEnvironment.MaxUpdateErrorsInARow)) {
            JobManager ! Error(job, e)
            JobManager ! Kill(job)
          }
          else {
            Log.logger.log(Log.FINE, s"${updateErrorsInARow + 1} errors in a row during job refresh", e)
            JobManager ! Delay(Refresh(job, sj, bj, delay, updateErrorsInARow + 1), delay)
          }
      }
    }
  }
}

