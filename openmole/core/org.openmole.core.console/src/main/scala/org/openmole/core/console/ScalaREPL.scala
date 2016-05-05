/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.console

import java.net.URLClassLoader
import java.util
import java.util.UUID

import org.openmole.core.exception._
import org.openmole.core.pluginmanager._
import org.osgi.framework.Bundle

import scala.annotation.tailrec
import scala.reflect.internal.util.{ NoPosition, Position }
import scala.tools.nsc._
import scala.tools.nsc.reporters._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.tools.nsc.interpreter._
import monocle.macros._
import scala.tools.nsc.io.AbstractFile

object ScalaREPL {
  @Lenses case class CompilationError(errorMessages: List[ErrorMessage], code: String, parent: Throwable) extends Exception(parent) {
    override def toString() = {
      def readableErrorMessages(error: ErrorMessage) =
        error.position.map(p ⇒ s"(line ${p.line}) ").getOrElse("") + error.decoratedMessage

      errorMessages.map(readableErrorMessages).mkString("\n") + "\n" +
        s"""Compiling code:
            |${code}""".stripMargin
    }
  }
  @Lenses case class ErrorMessage(decoratedMessage: String, rawMessage: String, position: Option[ErrorPosition])
  @Lenses case class ErrorPosition(line: Int, start: Int, end: Int, point: Int)

  def warmup = new ScalaREPL().eval("def warmup = {}")
  case class HeaderInfo(file: String)
  def firstLine(file: String) = HeaderInfo(file)
}

class REPLClassloader(val file: AbstractFile, parent: ClassLoader) extends scala.reflect.internal.util.AbstractFileClassLoader(file, parent) {

  def classFiles = {
    def all(fs: AbstractFile): List[AbstractFile] = {
      if (fs.isDirectory) fs :: fs.iterator.flatMap(all).toList
      else List(fs)
    }
    all(file).filterNot(_.isDirectory)
  }

  def toClassPath(c: String) = s"(memory)/${c.replace('.', '/')}.class"
  def findClassFile(name: String): Option[AbstractFile] = classFiles.find(_.path == toClassPath(name))
  def findClassFile(c: Class[_]): Option[AbstractFile] = findClassFile(c.getClass.getName)

  case class ReferencedClasses(repl: Vector[String], other: Vector[String])

  def referencedClasses(classes: Class[_]*) = {
    import org.openmole.tool.bytecode._

    val seen = collection.mutable.HashSet[String]()
    val toProcess = collection.mutable.Stack[String]()

    classes.foreach(c ⇒ toProcess.push(c.getName))

    while (!toProcess.isEmpty) {
      val processing = toProcess.pop
      for {
        classFile ← findClassFile(processing).toSeq
        refClass ← listAllClasses(classFile.toByteArray)
        if !seen.contains(refClass.getClassName)
      } {
        seen += refClass.getClassName
        toProcess push refClass.getClassName
      }
    }

    val (repl, other) = seen.toVector.partition(c ⇒ findClassFile(c).isDefined)
    ReferencedClasses(repl, other)
  }
}

import ScalaREPL._

class ScalaREPL(priorityBundles: ⇒ Seq[Bundle] = Nil, jars: Seq[JFile] = Seq.empty, quiet: Boolean = true) extends ILoop { repl ⇒

  var storeErrors: Boolean = true
  var errorMessage: List[ErrorMessage] = Nil
  var loopExitCode = 0

  System.setProperty("jline.shutdownhook", "true")
  override val prompt = "\nOpenMOLE> "

  lazy val firstLineTag = "/*" + UUID.randomUUID().toString + "*/"

  super.getClass.getMethods.find(_.getName.contains("globalFuture_$eq")).get.invoke(this, Future { true }.asInstanceOf[AnyRef])

  settings = new Settings
  //settings.Yreplclassbased.value = true
  settings.Yreplsync.value = true
  settings.verbose.value = false

  in = chooseReader(settings)

  private def messageToException(e: Throwable, messages: List[ErrorMessage], code: String): Throwable =
    CompilationError(messages.reverse, code, e)

  def eval(code: String) = synchronized {
    errorMessage = Nil
    try intp.eval("\n" + firstLineTag + "\n" + code)
    catch {
      case e: Throwable ⇒
        throw messageToException(e, errorMessage, code)
    }
  }

  def compile(code: String) = synchronized {
    errorMessage = Nil
    try intp.compile("\n" + firstLineTag + "\n" + code)
    catch {
      case e: Throwable ⇒
        throw messageToException(e, errorMessage, code)
    }
  }

  def loopWithExitCode = {
    loop()
    loopExitCode
  }

  def interpretAllFromWithExitCode(file: reflect.io.File) = {
    interpretAllFrom(file)
    loopExitCode
  }

  override def commands: List[LoopCommand] = super.commands.filter(_.name != "quit") ++ List(
    LoopCommand.cmd("quit", "[code]", "exit the application with a return code", exit)
  )

  def exit(s: String) = {
    if (!s.isEmpty) loopExitCode = s.toInt
    Result(keepRunning = false, None)
  }

  intp = new IMain {

    override lazy val reporter = new ReplReporter(this) {

      override def error(pos: Position, msg: String): Unit = {
        if (storeErrors) {
          val error =
            pos match {
              case NoPosition ⇒ ErrorMessage(msg, msg, None)
              case _ ⇒
                val compiled = new String(pos.source.content).split("\n")
                val firstLine = compiled.zipWithIndex.find { case (l, _) ⇒ l.contains(firstLineTag) }.map(_._2 + 1).getOrElse(0)
                val offset = compiled.take(firstLine).map(_.length + 1).sum
                def errorPos = ErrorPosition(pos.line - firstLine, pos.start - offset, pos.end - offset, pos.point - offset)
                def decoratedMessage = {
                  val offsetOfError = pos.point - compiled.take(pos.line - 1).map(_.length + 1).sum
                  s"""$msg
                  |${compiled(pos.line - 1)}
                  |${(" " * offsetOfError)}^""".stripMargin
                }

                ErrorMessage(decoratedMessage, msg, Some(errorPos))
            }

          errorMessage ::= error
        }
        super.error(pos, msg)
      }

      override def printMessage(msg: String) = if (!quiet) super.printMessage(msg)

    }

    override protected def newCompiler(settings: Settings, reporter: Reporter) = {
      //settings.Yreplclassbased.value = true
      settings.exposeEmptyPackage.value = true
      settings.outputDirs setSingleOutput replOutput.dir
      if (Activator.osgi) {
        new OSGiScalaCompiler(settings, reporter, replOutput.dir, priorityBundles, jars)
      }
      else {
        case class Plop()
        settings.embeddedDefaults[Plop]
        //settings.usejavacp.value = true
        super.newCompiler(settings, reporter)
      }
    }

    override lazy val classLoader =
      if (Activator.osgi) {
        new REPLClassloader(
          replOutput.dir,
          new CompositeClassLoader(
            priorityBundles.map(_.classLoader) ++
              List(new URLClassLoader(jars.toArray.map(_.toURI.toURL))) ++
              List(classOf[OSGiScalaCompiler].getClassLoader): _*
          )
        )
      }
      else super.classLoader
  }

}
