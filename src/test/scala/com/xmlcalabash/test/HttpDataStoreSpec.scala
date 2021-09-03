package com.xmlcalabash.test

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.util.stores.{DataInfo, DataReader, DataWriter, FallbackDataStore, FileDataStore, HttpDataStore}
import net.sf.saxon.s9api.XdmAtomicValue
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

import java.io.{File, InputStream, OutputStream}
import java.net.URI

class HttpDataStoreSpec extends AnyFlatSpec with BeforeAndAfter {
  private val config = XMLCalabashConfig.newInstance()
  private val httpStore = new HttpDataStore(config, new FallbackDataStore())
  private val baseURI = URI.create("http://localhost:8246/service/")
  private val testIO = new TestIO()

  "readEntry" should "pass" in {
    var pass = true
    try {
      httpStore.readEntry("fixed-xml", baseURI, "*/*", None, testIO)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  "writeEntry" should "pass" in {
    var pass = true
    try {
      httpStore.writeEntry("accept-put", baseURI, "text/plain", testIO)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  "infoEntry" should "pass" in {
    var pass = true
    try {
      httpStore.infoEntry("fixed-html", baseURI, "*/*", testIO)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  "listEachEntry" should "pass" in {
    var pass = true
    try {
      httpStore.listEachEntry("http://localhost/", baseURI, "*/*", testIO)
    } catch {
      case _: Throwable => pass = false
    }
    pass = pass && testIO.fileCount > 0
    assert(pass)
  }

  "createList" should "fail" in {
    var pass = false
    try {
      httpStore.createList("subdir", baseURI)
    } catch {
      case _: Exception =>
        pass = true
    }
    assert(pass)
  }

  "deleteEntry" should "pass" in {
    var pass = true
    try {
      httpStore.deleteEntry("accept-delete", baseURI)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  private class TestIO extends DataReader with DataWriter with DataInfo {
    var fileCount = 0

    override def load(id: URI, media: String, content: InputStream, len: Option[Long]): Unit = {
      // nop
    }

    override def store(content: OutputStream): Unit = {
      // nop
    }

    override def list(id: URI, props: Map[String, XdmAtomicValue]): Unit = {
      fileCount += 1
    }
  }
}
