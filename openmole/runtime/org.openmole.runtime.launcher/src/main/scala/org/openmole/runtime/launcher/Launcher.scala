/**
 * Created by Romain Reuillon on 29/03/16.
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
 *
 */
package org.openmole.runtime.launcher

import java.io.File
import java.util.ServiceLoader

import org.osgi.framework.Constants
import org.osgi.framework.launch._

import collection.JavaConversions._
import scala.annotation.tailrec
import scala.util.{ Success, Try, Failure }

object Launcher {

  def main(args: Array[String]): Unit = {
    case class Config(
      directory:     Option[File]   = None,
      run:           Option[String] = None,
      osgiDirectory: Option[String] = None,
      ignored:       List[String]   = Nil,
      args:          List[String]   = Nil
    )

    @tailrec def parse(args: List[String], c: Config = Config()): Config = args match {
      case "--plugins" :: tail        ⇒ parse(tail.tail, c.copy(directory = tail.headOption.map(new File(_))))
      case "--run" :: tail            ⇒ parse(tail.tail, c.copy(run = Some(tail.head)))
      case "--osgi-directory" :: tail ⇒ parse(tail.tail, c.copy(osgiDirectory = tail.headOption))
      case "--" :: tail               ⇒ parse(Nil, c.copy(args = tail))
      case s :: tail                  ⇒ parse(tail, c.copy(ignored = s :: c.ignored))
      case Nil                        ⇒ c
    }

    val config = parse(args.toList)

    val frameworkFactory = ServiceLoader.load(classOf[FrameworkFactory]).iterator().next()

    val osgiConfig = Map[String, String](
      (Constants.FRAMEWORK_STORAGE, ""),
      (Constants.FRAMEWORK_STORAGE_CLEAN, "true")
    ) ++ config.osgiDirectory.map(Constants.FRAMEWORK_STORAGE → _)

    val framework = frameworkFactory.newFramework(osgiConfig)
    framework.init()

    val ret = try {

      val context = framework.getBundleContext

      val bundles =
        for {
          f ← Option(config.directory.get.listFiles()).getOrElse(Array.empty)
        } yield context.installBundle(f.toURI.toString)

      bundles.foreach {
        _.start
      }

      def mains(clazz: String) =
        bundles.flatMap { b ⇒
          Try(b.loadClass(clazz).asInstanceOf[Class[Any]]).toOption
        }

      val mainClass =
        config.run match {
          case None ⇒ throw new RuntimeException(s"You should pass a run class argument")
          case Some(m) ⇒
            mains(m).toList match {
              case Nil      ⇒ throw new RuntimeException(s"Main class $m not found")
              case h :: Nil ⇒ h
              case _        ⇒ throw new RuntimeException(s"${m.size} run class $m have been found")
            }
        }

      Try(mainClass.getDeclaredMethod("run", classOf[Array[String]])) match {
        case Failure(_) ⇒ throw new RuntimeException(s"No run method with signature int run(s: Array[String]) has been found in ${mainClass.getCanonicalName}")
        case Success(m) ⇒
          if (java.lang.reflect.Modifier.isStatic(m.getModifiers)) {
            if (!classOf[Int].isAssignableFrom(m.getReturnType)) throw new RuntimeException(s"Method run should return an int instead of ${m.getReturnType}")
            m.invoke(null, config.args.toArray).asInstanceOf[Int]
          }
          else throw new RuntimeException(s"Method run should be static")
      }
    }
    finally framework.stop()

    sys.exit(ret)
  }

}