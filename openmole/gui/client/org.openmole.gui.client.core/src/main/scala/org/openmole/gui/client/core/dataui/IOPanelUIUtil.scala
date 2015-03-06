package org.openmole.gui.client.core.dataui

import org.openmole.gui.client.core.{ PrototypeFactoryUI, ClientService }
import org.openmole.gui.misc.js.Forms._
import org.openmole.gui.misc.js.{ ClassKeyAggregator, InputFilter }
import org.scalajs.dom.html._

import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }
import org.openmole.gui.misc.js.{ Forms ⇒ bs }

/*
 * Copyright (C) 05/03/15 // mathieu.leclaire@openmole.org
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

object IOPanelUIUtil {

  def filtered(ifilter: InputFilter, dataUI: InOutputDataUI, mappingsFactory: IOMappingsFactory) = ClientService.prototypeDataBagUIs.map {
    p ⇒ inoutputUI(p, mappingsFactory)
  }.filter { i ⇒
    ifilter.contains(i.protoDataBagUI.name()) &&
      !ifilter.nameFilter().isEmpty &&
      !dataUI.exists(i)
  }

  def prototypeHeaderSequence: List[String] = List("Name", "Type", "Dim")

  def buildHeaders(headers: Seq[String]) = thead(
    tags.tr(
      for (k ← headers) yield {
        tags.th(k)
      }
    )
  )

  def buildProto(name: String) = {
    val newProto = DataBagUI.prototype(PrototypeFactoryUI.doubleFactory, name)
    ClientService += newProto
    newProto
  }

  def buildPrototypeTableView(io: InOutputUI, todo: ⇒ Unit): Seq[TypedTag[TableCell]] = Seq(
    clickablePrototypeTD(io.protoDataBagUI, () ⇒ todo),
    labelTD(io.protoDataBagUI.dataUI().dataType, label_primary),
    basicTD(io.protoDataBagUI.dataUI().dimension().toString)
  ) ++ mappingsTD(io)

  def clickablePrototypeTD(p: PrototypeDataBagUI, todo: () ⇒ Unit) = bs.td(col_md_2)(
    a(p.name(),
      cursor := "pointer",
      onclick := {
        println("TODO ")
        todo
      }))

  def emptyTD(nb: Int) = for (i ← (0 to nb - 1)) yield {
    bs.td(col_md_1)("")
  }

  def labelTD(s: String, labelType: ClassKeyAggregator) = bs.td(col_md_1)(bs.label(s, labelType))

  def basicTD(s: String) = bs.td(col_md_1)(tags.span(s))

  def mappingsTD(i: InOutputUI) = for (
    f ← i.mappings().fields.map {
      _.panelUI
    }
  ) yield {
    tags.td(f.view)
  }

  def delButtonTD(todo: ⇒ Unit) = bs.td(col_md_1)(bs.button(glyph(glyph_minus))(onclick := {
    () ⇒ todo
    //dataUI -= io
  }
  ))

  def coloredTR(tds: Seq[TypedTag[TableCell]], filter: () ⇒ Boolean) = bs.tr(
    if (filter()) warning
    else nothing
  )(tds)

  def saveInOutputsUI(inouts: Seq[InOutputUI]) = {
    inouts.map {
      _.mappings().fields.map {
        _.panelUI.save
      }
    }
  }

}