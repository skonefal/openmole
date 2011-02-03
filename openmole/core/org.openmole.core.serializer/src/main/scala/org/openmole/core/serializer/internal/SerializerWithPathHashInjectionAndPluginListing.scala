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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.serializer.internal

import java.io.File
import org.openmole.commons.tools.io.FileUtil.fileOrdering
import org.openmole.commons.tools.io.StringInputStream
import org.openmole.core.serializer.FileInfoHash
import scala.collection.immutable.TreeMap
import Activator._

class SerializerWithPathHashInjectionAndPluginListing extends SerializerWithPluginClassListing {

  var files = new TreeMap[File, FileInfoHash]
  registerConverter(new FilePathHashNotifier(this))
  
  def fileUsed(file: File): FileInfoHash = {
    var hash = files.getOrElse(file,
                               {
        val pathHash = hashService.computeHash(new StringInputStream(file.getAbsolutePath()))
        val fileHash = hashService.computeHash(file)
            
        val hash = new FileInfoHash(fileHash, pathHash)
            
        files += file -> hash
        hash
      })
    hash
  }
    
  override def clean: Unit = {
    files = new TreeMap[File, FileInfoHash]
    super.clean
  }
    
}
