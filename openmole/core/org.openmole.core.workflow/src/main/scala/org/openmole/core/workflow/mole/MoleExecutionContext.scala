/*
 * Copyright (C) 22/02/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.mole

import java.io.{ File, PrintStream }

import org.openmole.core.event.EventDispatcher
import org.openmole.core.fileservice.FileService
import org.openmole.core.output.OutputManager
import org.openmole.core.preference.Preference
import org.openmole.core.serializer._
import org.openmole.core.threadprovider._
import org.openmole.core.workspace._
import org.openmole.core.workflow.dsl._
import org.openmole.tool.cache._
import org.openmole.tool.random.Seeder

object MoleExecutionContext {
  def apply(
    out: PrintStream = OutputManager.systemOutput
  )(implicit moleServices: MoleServices) = new MoleExecutionContext(out)
}

class MoleExecutionContext(
  val out: PrintStream
)(implicit val services: MoleServices)

object MoleServices {

  implicit def create(implicit preference: Preference, seeder: Seeder, threadProvider: ThreadProvider, eventDispatcher: EventDispatcher, newFile: NewFile, fileService: FileService, workspace: Workspace) = {
    new MoleServices()(
      preference = preference,
      seeder = Seeder(seeder.newSeed),
      threadProvider = threadProvider,
      eventDispatcher = eventDispatcher,
      newFile = NewFile(newFile.newDir("execution")),
      workspace = workspace,
      fileService = fileService
    )
  }
}

class MoleServices(
    implicit
    val preference:      Preference,
    val seeder:          Seeder,
    val threadProvider:  ThreadProvider,
    val eventDispatcher: EventDispatcher,
    val newFile:         NewFile,
    val workspace:       Workspace,
    val fileService:     FileService
) {
  def newRandom = Lazy(seeder.newRNG)
  implicit lazy val defaultRandom = newRandom
}
