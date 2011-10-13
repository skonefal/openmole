/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.workflow

import java.awt.Color
import java.awt.Container
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import org.netbeans.api.visual.widget.Widget
import org.openide.util.ImageUtilities
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.commons.Constants
import org.openmole.ide.core.model.workflow.IMoleScene

class SamplingWidget(scene: IMoleScene,val capsule: ICapsuleUI ) extends Widget(scene.graphScene) {
  val titleArea = new Rectangle
  titleArea.setBounds(new Rectangle(0,33,33,Constants.TASK_TITLE_HEIGHT/2))
  
  override def paintWidget= {
    super.paintWidget
    val sampling = capsule.dataProxy.get.dataUI.sampling
    if (sampling.isDefined){
      val g = getGraphics.asInstanceOf[Graphics2D]
      g.drawImage(ImageUtilities.loadImage(sampling.get.dataUI.imagePath),0,0,new Container)
      
      g.setColor(new Color(77,77,77))
      g.setFont(new Font("Ubuntu", Font.PLAIN, 10))
      
      val rect = g.getFontMetrics(g.getFont).getStringBounds(sampling.get.dataUI.name, g)
      g.drawString(sampling.get.dataUI.name,
                   ((titleArea.width- rect.getWidth)/2).asInstanceOf[Int],40)
    }
  }
  
  def setDetailedView(w: Int)= setPreferredLocation(new Point(w/2-15,Constants.TASK_CONTAINER_HEIGHT+10))
}