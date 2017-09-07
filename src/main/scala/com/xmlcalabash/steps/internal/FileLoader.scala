package com.xmlcalabash.steps.internal

import java.io.File
import java.net.URLConnection
import java.nio.file.Files

import com.jafpl.exceptions.PipelineException
import com.xmlcalabash.runtime.{XmlMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultStep
import net.sf.saxon.s9api.XdmItem

class FileLoader() extends DefaultStep {
  private var _href = ""
  private var docProps = Map.empty[String, String]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(variable: String, value: Any): Unit = {
    config.get.trace("debug", s"FileLoader receives binding: $variable: $value", "stepBindings")

    variable match {
      case "document-properties" =>
        value match {
          case item: XdmItem =>
            docProps = parseDocumentProperties(item)
          case _ => throw new PipelineException("badtype", "document properties must be an item", None)
        }
      case "href" =>
        _href = value.toString
      case _ =>
        logger.info("Ignoring unexpected option to p:document: " + variable)
    }
  }

  override def run(): Unit = {
    // Using the filename sort of sucks, but it's what the OSes do at this point so...sigh
    // You can extend the set of known extensions by pointing the system property
    // `content.types.user.table` at your own mime types file. The default file to
    // start with is in $JAVA_HOME/lib/content-types.properties
    val contentType = Option(URLConnection.guessContentTypeFromName(_href))

    // I'm not sure what to do here...
    try {
      val node = config.get.documentManager.parse(_href)
      val ctype = contentType.getOrElse("application/xml")
      consumer.get.receive("result", node, new XmlMetadata(ctype, docProps))
    } catch {
      case t: Throwable =>
        // What should the representation of non-XML data be?
        val bytes = Files.readAllBytes(new File(_href).toPath)
        val ctype = contentType.getOrElse("application/octet-stream")
        consumer.get.receive("result", bytes, new XmlMetadata(ctype))
    }
  }
}
