package com.xmlcalabash.drivers

import java.io.{BufferedInputStream, BufferedReader, ByteArrayOutputStream, File, FileReader, InputStreamReader}
import java.util.Base64
import javax.xml.transform.sax.SAXSource

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.TestException
import com.xmlcalabash.testers.TestRunner
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{QName, Serializer, XdmAtomicValue, XdmDestination}
import org.xml.sax.InputSource

object TestDriver extends App {
  private val xmlCalabash = XMLCalabash.newInstance()

  private val xmlOutput: Option[String] = None
  private val htmlOutput: Option[String] = Some("test-suite-report.html")

  val singleTest: Option[String] = None // Some("ab-with-input-036.xml")

  val runner = if (singleTest.isEmpty) {
    new TestRunner(xmlCalabash, "src/test/resources/test-suite/test-suite/tests")
  } else {
    new TestRunner(xmlCalabash, "src/test/resources/test-suite/test-suite/tests/" + singleTest.get)
  }

  try {
    val junit = runner.junit()

    if (xmlOutput.isDefined) {
      val serializer = xmlCalabash.processor.newSerializer()
      serializer.setOutputFile(new File(xmlOutput.get))
      serializer.setOutputProperty(Serializer.Property.METHOD, "xml")
      S9Api.serialize(xmlCalabash, junit, serializer)
    }

    if (htmlOutput.isDefined) {
      // Pass in the CSS and JS as base64 encoded parameters so that
      // the test report document can use data: URIs and avoid links
      val cssstream = getClass.getResourceAsStream("/testsuite.css")
      var baos = new ByteArrayOutputStream()
      var data = new Array[Byte](4096)
      var nread = cssstream.read(data, 0, data.length)
      while (nread >= 0) {
        baos.write(data, 0, nread)
        nread = cssstream.read(data, 0, data.length)
      }
      baos.flush()
      val css64 = Base64.getEncoder.encodeToString(baos.toByteArray)

      val jsstream = getClass.getResourceAsStream("/testsuite.js")
      baos = new ByteArrayOutputStream()
      nread = jsstream.read(data, 0, data.length)
      while (nread >= 0) {
        baos.write(data, 0, nread)
        nread = jsstream.read(data, 0, data.length)
      }
      baos.flush()
      val js64 = Base64.getEncoder.encodeToString(baos.toByteArray)

      val stystream = getClass.getResourceAsStream("/testsuite.xsl")
      if (stystream == null) {
        throw new TestException("Failed to load testsuite.xsl from resources.")
      }
      val stylesheet = new SAXSource(new InputSource(stystream))

      val compiler = xmlCalabash.processor.newXsltCompiler()
      compiler.setSchemaAware(false)

      val exec = compiler.compile(stylesheet)
      val transformer = exec.load()
      transformer.setInitialContextNode(junit)
      val result = new XdmDestination()
      transformer.setDestination(result)
      transformer.setParameter(new QName("", "css"), new XdmAtomicValue(css64))
      transformer.setParameter(new QName("", "js"), new XdmAtomicValue(js64))

      transformer.transform()

      val serializer = xmlCalabash.processor.newSerializer()
      serializer.setOutputFile(new File(htmlOutput.get))
      serializer.setOutputProperty(Serializer.Property.METHOD, "html")
      S9Api.serialize(xmlCalabash, result.getXdmNode, serializer)
    }
  } catch {
    case t: Throwable => println(t)
  }
}
