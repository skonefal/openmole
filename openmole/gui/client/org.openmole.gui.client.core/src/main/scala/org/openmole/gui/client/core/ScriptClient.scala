package org.openmole.gui.client.core

import org.openmole.gui.client.core.dataui.EditorPanelUI
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.Forms._
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.misc.js.JsRxTags._

import org.scalajs.dom

import scalatags.JsDom.all._
/*
 * Copyright (C) 15/04/15 // mathieu.leclaire@openmole.org
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

@JSExport("ScriptClient")
object ScriptClient {

  @JSExport
  def run(): Unit = {

    lazy val editor = EditorPanelUI(Seq(
      ("Compile", "Enter", () ⇒ println("Compile  !"))
    ),
      ""
    )

    val topdiv = dom.document.body.appendChild(tags.div.render)

    topdiv.appendChild(
      nav("mainMav",
        Seq(
          (navItem("executions", "Executions").render, "env", () ⇒ {
            println("Not yet")
          })
        ), nav_pills + nav_inverse + nav_staticTop
      )
    )

    topdiv.appendChild(editor.view.render)
    dom.document.body.appendChild(topdiv)

    editor.init

  }
}