package com.xmlcalabash.test

import java.io.{File, InputStream, OutputStream}
import java.net.URI

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.util.stores.{DataInfo, DataReader, DataWriter, FallbackDataStore, FileDataStore}
import net.sf.saxon.s9api.XdmAtomicValue
import org.scalatest.{BeforeAndAfter, FlatSpec}

class FileDataStoreSpec extends FlatSpec with BeforeAndAfter {
  private val config = XMLCalabash.newInstance()
  private val fileStore = new FileDataStore(new FallbackDataStore())
  private val testIO = new TestIO()
  private val tempDir: File = File.createTempFile("meerschaum-test-", ".dir")
  private var tempFile: File = null

  before {
    tempDir.delete()
    tempDir.mkdir()
    tempFile = File.createTempFile("meerschaum-test-", ".bin", tempDir)
  }

  after {
    tempFile.delete()
    tempDir.delete()
  }

  "readEntry" should "pass" in {
    var pass = true
    try {
      fileStore.readEntry(tempFile.getName, tempDir.toURI, "*/*", None, testIO)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  "writeEntry" should "pass" in {
    var pass = true
    try {
      fileStore.writeEntry("foo.txt", tempDir.toURI, "text/plain", testIO)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  "infoEntry" should "pass" in {
    var pass = true
    try {
      fileStore.infoEntry(tempFile.getName, tempDir.toURI, "*/*", testIO)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  "listEachEntry" should "pass" in {
    var pass = true
    try {
      fileStore.listEachEntry("", tempDir.toURI, "*/*", testIO)
    } catch {
      case _: Throwable => pass = false
    }
    pass = pass && testIO.sawFile
    assert(pass)
  }

  "createList" should "pass" in {
    var pass = true
    try {
      fileStore.createList("subdir", tempDir.toURI)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  "deleteEntry" should "pass" in {
    var pass = true
    try {
      fileStore.deleteEntry("foo.txt", tempDir.toURI)
      fileStore.deleteEntry("subdir", tempDir.toURI)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  private class TestIO extends DataReader with DataWriter with DataInfo {
    var sawFile = false

    override def load(id: URI, media: String, content: InputStream, len: Long): Unit = {
      // nop
    }

    override def store(content: OutputStream): Unit = {
      // nop
    }

    override def list(id: URI, props: Map[String, XdmAtomicValue]): Unit = {
      sawFile = id.toASCIIString.endsWith("/foo.txt")
    }
  }

}
