/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.implementation.dialog

import javax.swing._
import java.awt.Dimension
import javax.swing.JOptionPane._
import scala.swing.ScrollPane
import scala.swing.ScrollPane._
import org.openmole.ide.core.implementation.workflow.DataChannelConnectionWidget
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor

object DataChannelDialog {
  def display(dcWidget: DataChannelConnectionWidget) = {
    openable match {
      case true ⇒
        val prototypePanel = new PrototypePanel(dcWidget.dataChannelUI.availablePrototypes,
          dcWidget.dataChannelUI.filteredPrototypes)
        if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(prototypePanel) {
          verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
        }.peer,
          "Set the Data Channel")).equals(NotifyDescriptor.OK_OPTION))
          dcWidget.dataChannelUI.filteredPrototypes = prototypePanel.multiPrototypeCombo.content
      case false ⇒ StatusBar.warn("No Prototype is defined !")
    }

    def openable =
      dcWidget.dataChannelUI.source.dataUI.task match {
        case Some(x: ITaskDataProxyUI) ⇒
          !(x.dataUI.prototypesOut ++
            x.dataUI.implicitPrototypesOut).isEmpty
        case None ⇒ false
      }
  }

  class PrototypePanel(availableProtoProxys: List[IPrototypeDataProxyUI],
                       protoProxys: List[IPrototypeDataProxyUI]) extends PluginPanel("") {
    preferredSize = new Dimension(250, 300)
    val multiPrototypeCombo = new MultiCombo("Filtered Prototypes",
      availableProtoProxys,
      protoProxys,
      CLOSE_IF_EMPTY,
      ADD)
    if (protoProxys.isEmpty) multiPrototypeCombo.removeAllRows
    contents += multiPrototypeCombo.panel
  }
}
