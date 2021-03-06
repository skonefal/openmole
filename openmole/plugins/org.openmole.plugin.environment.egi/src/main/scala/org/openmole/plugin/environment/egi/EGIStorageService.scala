/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.environment.egi

import java.io.{ File, IOException, InputStream }
import java.net.URI

import fr.iscpif.gridscale.http.{ DPMWebDAVStorage, HTTPSAuthentication, WebDAVLocation }
import fr.iscpif.gridscale.storage.{ ListEntry, Storage ⇒ GSStorage }
import org.openmole.core.communication.storage.{ RemoteStorage, _ }
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.plugin.environment.batch.control._
import org.openmole.plugin.environment.batch.environment.BatchEnvironment
import org.openmole.plugin.environment.batch.storage._
import org.openmole.plugin.environment.gridscale.GridScaleStorage
import org.openmole.tool.file._
import squants.time.Time

import scala.sys.process.{ Process, ProcessLogger }
import scala.util.Try

trait EGIStorageService extends StorageService with GridScaleStorage with CompressedTransfer {
  val usageControl: AvailabilityQuality
  import usageControl.quality

  override def exists(path: String)(implicit token: AccessToken): Boolean = quality { super.exists(path)(token) }
  override def listNames(path: String)(implicit token: AccessToken): Seq[String] = quality { super.listNames(path)(token) }
  override def list(path: String)(implicit token: AccessToken): Seq[ListEntry] = quality { super.list(path)(token) }
  override def makeDir(path: String)(implicit token: AccessToken): Unit = quality { super.makeDir(path)(token) }
  override def rmDir(path: String)(implicit token: AccessToken): Unit = quality { super.rmDir(path)(token) }
  override def rmFile(path: String)(implicit token: AccessToken): Unit = quality { super.rmFile(path)(token) }
  override def downloadStream(path: String, transferOptions: TransferOptions)(implicit token: AccessToken): InputStream = quality { super.downloadStream(path, transferOptions)(token) }
  override def uploadStream(is: InputStream, path: String, transferOptions: TransferOptions)(implicit token: AccessToken) = quality { super.uploadStream(is, path, transferOptions)(token) }
}

trait NativeCommandCopy {

  def url: URI

  protected def run(cmd: String) = {
    val output = new StringBuilder
    val error = new StringBuilder

    val logger =
      ProcessLogger(
        (o: String) ⇒ output.append("\n" + o),
        (e: String) ⇒ error.append("\n" + e)
      )

    val exit = Process(Seq("bash", "-c", cmd)) ! logger
    if (exit != 0) throw new RuntimeException(s"Command $cmd had a non 0 return value.\n Output: ${output.toString}. Error: ${error.toString}")
    output.toString
  }

  def child(parent: String, child: String): String = GSStorage.child(parent, child)

  def download(src: String, dest: File, options: TransferOptions)(implicit newFile: NewFile): Unit =
    try {
      if (options.raw) download(src, dest)
      else newFile.withTmpFile { tmpFile ⇒
        download(src, tmpFile)
        tmpFile.copyUncompressFile(dest)
      }
    }
    catch {
      case e: Throwable ⇒ throw new IOException(s"Error downloading $src to $dest from $url with option $options", e)
    }

  private def download(src: String, dest: File): Unit = run(downloadCommand(url.resolve(src), dest.getAbsolutePath))

  def upload(src: File, dest: String, options: TransferOptions)(implicit newFile: NewFile): Unit =
    try {
      if (options.raw) upload(src, dest)
      else newFile.withTmpFile { tmpFile ⇒
        src.copyCompressFile(tmpFile)
        upload(tmpFile, dest)
      }
    }
    catch {
      case e: Throwable ⇒ throw new IOException(s"Error uploading $src to $dest from $url with option $options", e)
    }

  private def upload(src: File, dest: String): Unit = run(uploadCommand(src.getAbsolutePath, url.resolve(dest)))

  def downloadCommand(from: URI, to: String): String
  def uploadCommand(from: String, to: URI): String
}

object EGIWebDAVStorageService {

  def apply[A: HTTPSAuthentication](s: WebDAVLocation, _environment: BatchEnvironment, voName: String, debug: Boolean, authentication: A)(implicit preference: Preference, threadProvider: ThreadProvider, newFile: NewFile) = {
    val storage =
      new EGIWebDAVStorageService {
        def threads = preference(EGIEnvironment.ConnectionsByWebDAVSE)

        val usageControl = AvailabilityQuality(new LimitedAccess(threads, Int.MaxValue), preference(EGIEnvironment.QualityHysteresis))
        val storage = DPMWebDAVStorage(s.copy(basePath = ""))(authentication)
        val url = new URI("https", null, s.host, s.port, null, null, null)
        val remoteStorage = new CurlRemoteStorage(s.host, s.port, voName, preference(EGIEnvironment.RemoteCopyTimeout), debug)
        val environment = _environment
        val root = s.basePath
        val id = new URI("webdavs", voName, s.host, s.port, s.basePath, null, null).toString
      }
    StorageService.startGC(storage)
    storage
  }
}

trait EGIWebDAVStorageService <: EGIStorageService

class CurlRemoteStorage(val host: String, val port: Int, val voName: String, val timeout: Time, val debug: Boolean) extends RemoteStorage with NativeCommandCopy { s ⇒
  lazy val curl = new Curl(voName, debug, timeout)

  @transient lazy val url = new URI("https", null, host, port, null, null, null)
  def downloadCommand(from: URI, to: String): String = curl.download(from, to)
  def uploadCommand(from: String, to: URI): String = curl.upload(from, to)

  override def upload(src: File, dest: String, options: TransferOptions)(implicit newFile: NewFile): Unit =
    try super.upload(src, dest, options)
    catch {
      case t: Throwable ⇒
        Try(run(s"${curl.curl} -X DELETE ${url.resolve(dest)}"))
        throw t
    }
}