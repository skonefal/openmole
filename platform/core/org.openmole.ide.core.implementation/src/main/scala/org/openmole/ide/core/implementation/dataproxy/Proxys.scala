/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.dataproxy

import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.data._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashSet

object Proxys {
    
  var task = new HashSet[ITaskDataProxyUI]
  var prototype = new HashSet[IPrototypeDataProxyUI]
  var sampling = new HashSet[ISamplingDataProxyUI]
  var environment = new HashSet[IEnvironmentDataProxyUI]
  
  def isExplorationTaskData(pud: ITaskDataUI) = pud.coreClass.isAssignableFrom(classOf[ExplorationTask]) 

  def getTaskDataProxyUI(name: String) = task.filter(_.dataUI.name==name).headOption
  def getPrototypeDataProxyUI(name: String) = prototype.filter(_.dataUI.name==name).headOption
  def getSamplingDataProxyUI(name: String) = sampling.filter(_.dataUI.name==name).headOption
  def getEnvironmentDataProxyUI(name: String) = environment.filter(_.dataUI.name==name).headOption
  
  def getPrototypesNames = task.groupBy(_.dataUI.name).keys.toSet
  
  def addTaskElement(dpu: ITaskDataProxyUI) = task += dpu
  def addPrototypeElement(dpu: IPrototypeDataProxyUI) = prototype += dpu
  def addSamplingElement(dpu: ISamplingDataProxyUI) = sampling += dpu
  def addEnvironmentElement(dpu: IEnvironmentDataProxyUI) = environment += dpu
  
  
  def removeTaskElement(dpu: ITaskDataProxyUI) = task.remove(dpu)
  def removePrototypeElement(dpu: IPrototypeDataProxyUI) = prototype.remove(dpu)
  def removeSamplingElement(dpu: ISamplingDataProxyUI) = sampling.remove(dpu)
  def removeEnvironmentElement(dpu: IEnvironmentDataProxyUI) = environment.remove(dpu)
  
  def clearAllTaskElement = task.clear
  def clearAllPrototypeElement = prototype.clear
  def clearAllSamplingElement = sampling.clear
  def clearAllEnvironmentElement = environment.clear
  
  def clearAll: Unit = {
    clearAllTaskElement
    clearAllPrototypeElement
    clearAllSamplingElement
    clearAllEnvironmentElement
  }
  
} 
  
