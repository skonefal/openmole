package root.gui.plugin

import root.base
import sbt._
import root.gui._
import Keys._
import root._
import scala.scalajs.sbtplugin.ScalaJSPlugin._

import com.typesafe.sbt.osgi.OsgiKeys._

object Util {
  def project(rootDir: sbt.File,
              suffix: String,
              extProjectDependencies: Seq[ClasspathDep[ProjectReference]] = Seq(),
              extLibDependencies: Seq[ModuleID] = Seq(),
              clientProjectDependencies: Seq[ClasspathDep[ProjectReference]] = Seq(),
              clientLibDependencies: Seq[ModuleID] = Seq(),
              serverProjectDependencies: Seq[ClasspathDep[ProjectReference]] = Seq(),
              serverLibDependencies: Seq[ModuleID] = Seq()) = {
    lazy val ext = subProject(rootDir, suffix + ".ext", extProjectDependencies, extLibDependencies ++ Seq(root.Libraries.scalaTags)) dependsOn (Ext.data) settings (scalaJSSettings: _*)
    lazy val client = subProject(rootDir, suffix + ".client", clientProjectDependencies, clientLibDependencies) dependsOn (ext, Ext.dataui, Ext.factoryui, Misc.js, Bootstrap.osgi, base.Misc.replication % "test") settings (scalaJSSettings: _*)
    lazy val server = subProject(rootDir, suffix + ".server", serverProjectDependencies, serverLibDependencies) dependsOn (ext, Server.factory, base.Misc.replication % "test")

    //FIXME: how to call directly OsgiProject from here ?
    // OsgiProject(suffix) dependsOn (ext, client, server)

    (ext, client, server)
  }

  def subProject(rootDir: sbt.File,
                 suffix: String,
                 extProjectDependencies: Seq[ClasspathDep[ProjectReference]] = Seq(),
                 extLibDependencies: Seq[ModuleID] = Seq()) =
    Project(suffix.replace('.', '-'), new File(rootDir + "." + suffix)) settings (libraryDependencies ++= extLibDependencies) dependsOn (extProjectDependencies: _*)

}