/*
 * Copyright (C) 2011 leclaire
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

package org.openmole.ide.plugin.hook.file

import java.io.File
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.core.model.data.IHookDataUI
import org.openmole.plugin.hook.file.CopyFileHook

class CopyFileHookDataUI(executionManager: IExecutionManager,
                         toBeHooked: (ICapsule, IPrototype[File], String)) extends IHookDataUI {

  override def coreObject = new CopyFileHook(executionManager.moleExecution,
    toBeHooked._1,
    toBeHooked._2,
    toBeHooked._3)
}
