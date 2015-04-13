/*
 * Copyright (C) 2011 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.console

import jline.console.ConsoleReader
import java.util.concurrent.Executors
import org.openmole.core.console.ScalaREPL
import org.openmole.core.dsl.Serializer
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.logging.LoggerService
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workflow.tools.PluginInfo
import org.openmole.core.workspace.Workspace
import scala.annotation.tailrec
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{ NamedParam, ILoop, JLineCompletion, JLineReader }
import org.openmole.core.workflow.task._
import java.util.concurrent.TimeUnit
import scala.tools.nsc.io.{ File ⇒ SFile }
import java.io.File

object Console {

  @tailrec def askPassword(msg: String = "password"): String = {
    val password = new ConsoleReader().readLine(s"$msg             :", '*')
    val confirmation = new ConsoleReader().readLine(s"$msg confirmation:", '*')
    if (password != confirmation) {
      println("Password and confirmation don't match.")
      askPassword(msg)
    }
    else password
  }

  def setPassword(password: String) =
    try {
      Workspace.setPassword(password)
      true
    }
    catch {
      case e: UserBadDataError ⇒
        println("Password incorrect.")
        false
    }

  @tailrec def initPassword: Unit = {
    if (Workspace.passwordChosen && Workspace.passwordIsCorrect("")) setPassword("")
    else {
      val password =
        if (Workspace.passwordChosen) new ConsoleReader().readLine("Enter your OpenMOLE password (for preferences encryption): ", '*')
        else {
          println("OpenMOLE Password for preferences encryption has not been set yet, choose a  password.")
          askPassword("Preferences password")
        }
      if (!setPassword(password)) initPassword
    }
  }

}

import Console._

class ConsoleArgs(val args: Seq[String])
class Console(plugins: PluginSet = PluginSet.empty, password: Option[String] = None, script: List[String] = Nil) { console ⇒

  def workspace = "workspace"
  def registry = "registry"
  def logger = "logger"
  def serializer = "serializer"
  def commandsName = "_commands_"
  def pluginsName = "_plugins_"
  def variablesName = "_variables_"
  def argsName = "args"

  def autoImports: Seq[String] = PluginInfo.pluginsInfo.toSeq.flatMap(_.namespaces).map(n ⇒ s"$n._")
  def imports =
    Seq(
      "org.openmole.core.dsl._",
      s"$commandsName._"
    ) ++ autoImports

  def initialisationCommands =
    Seq(
      s"implicit lazy val plugins = $pluginsName",
      imports.map("import " + _).mkString("; ")
    )

  def run(args: Seq[String]) = {
    val correctPassword =
      password match {
        case None ⇒
          initPassword; true
        case Some(p) ⇒ setPassword(p)
      }

    if (correctPassword) withREPL { loop ⇒
      loop.beQuietDuring {
        loop.bind(variablesName, new ConsoleArgs(args))
        loop.eval(s"import $variablesName._")
      }
      script match {
        case Nil ⇒ loop.loop
        case scripts ⇒
          scripts.foreach { s ⇒
            val scriptFile = new File(s)
            if (scriptFile.exists) loop.interpretAllFrom(new SFile(scriptFile))
            else println("File " + scriptFile + " doesn't exist.")
          }
      }
    }
  }

  def initialise(loop: ScalaREPL) = {
    loop.beQuietDuring {
      loop.bind(commandsName, new Command)
      loop.bind(pluginsName, plugins)
      initialisationCommands.foreach { loop.interpret }
    }
    loop
  }

  def newREPL() = {
    val loop = new ScalaREPL(priorityClasses = List(this.getClass))
    initialise(loop)
  }

  def withREPL[T](f: ScalaREPL ⇒ T) = {
    val loop = newREPL()
    try f(loop)
    finally loop.close
  }

}