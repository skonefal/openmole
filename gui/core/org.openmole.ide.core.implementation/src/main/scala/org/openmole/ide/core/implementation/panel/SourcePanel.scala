/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.panel

import org.openmole.ide.core.model.dataproxy.ISourceDataProxyUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode
import swing._
import event.{ SelectionChanged, FocusGained }
import javax.swing.ImageIcon
import javax.imageio.ImageIO
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dialog.StatusBar
import java.awt.BorderLayout
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.ComponentFocusedEvent
import org.openmole.ide.core.implementation.prototype.UpdatedPrototypeEvent

class SourcePanel(proxy: ISourceDataProxyUI,
                  scene: IMoleScene,
                  mode: PanelMode.Value) extends BasePanel(Some(proxy), scene, mode) {
  hookPanel ⇒

  val panelUI = proxy.dataUI.buildPanelUI
  tabbedPane.pages.insert(1, new TabbedPane.Page("Inputs / Outputs", new Label))

  iconLabel.icon = new ImageIcon(ImageIO.read(this.getClass.getClassLoader.getResource("img/source.png")))

  def create = {
    Proxys += proxy
    scene.manager.invalidateCache
    ConceptMenu.sourceMenu.popup.contents += ConceptMenu.addItem(nameTextField.text, proxy)
  }

  def delete = {
    scene.closePropertyPanel
    Proxys -= proxy
    ConceptMenu.removeItem(proxy)
    true
  }

  def save = {
    val protoPanelSave = protoPanel.save
    proxy.dataUI = panelUI.save(nameTextField.text, protoPanelSave._1, protoPanelSave._2, protoPanelSave._3)
  }

  def buildProtoPanel = {
    val (implicitIP, implicitOP) = proxy.dataUI.implicitPrototypes
    new IOPrototypePanel(scene,
      this,
      proxy.dataUI.inputs,
      proxy.dataUI.outputs,
      implicitIP,
      implicitOP,
      proxy.dataUI.inputParameters.toMap)
  }

  def updateProtoPanel = {
    save
    protoPanel = buildProtoPanel
    tabbedPane.pages(1).content = protoPanel
  }

  var protoPanel = buildProtoPanel

  tabbedPane.pages(1).content = protoPanel
  tabbedPane.revalidate

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    contents += tabbedPane
    contents += panelUI.help
  }.peer, BorderLayout.CENTER)

  listenTo(panelUI.help.components.toSeq: _*)
  listenTo(tabbedPane.selection)
  reactions += {
    case FocusGained(source: Component, _, _) ⇒ panelUI.help.switchTo(source)
    case ComponentFocusedEvent(source: Component) ⇒ panelUI.help.switchTo(source)
    case SelectionChanged(tabbedPane) ⇒ updateProtoPanel
    case UpdatedPrototypeEvent(_) ⇒
      scene.closeExtraPropertyPanel
      updateProtoPanel
    case _ ⇒
  }
}