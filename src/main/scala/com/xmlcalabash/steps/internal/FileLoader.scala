package com.xmlcalabash.steps.internal

import java.io.File
import java.net.URLConnection
import java.nio.file.Files

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.{BindingMessage, ItemMessage}
import com.xmlcalabash.model.util.ValueParser
import com.xmlcalabash.runtime.{XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem}

class FileLoader() extends DefaultStep {
  private var _href = ""
  private var docProps = Map.empty[QName, XdmItem]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(bindmsg: BindingMessage): Unit = {
    val variable = bindmsg.name

    var valueitem = Option.empty[XdmItem]
    bindmsg.message match {
      case itemmsg: ItemMessage =>
        itemmsg.item match {
          case item: XdmItem =>
            valueitem = Some(item)
          case _ => Unit
        }
      case _ => Unit
    }

    if (valueitem.isEmpty) {
      throw new PipelineException("badtype", s"binding for $variable must be an item", None)
    }

    variable match {
      case "document-properties" =>
        docProps = ValueParser.parseDocumentProperties(valueitem.get, location)
      case "href" =>
        _href = valueitem.get.getStringValue
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
      logger.debug(s"Loaded ${_href} as $ctype")
      consumer.get.receive("result", new ItemMessage(node, new XProcMetadata(ctype, docProps)))
    } catch {
      case t: Throwable =>
        // What should the representation of non-XML data be?
        val bytes = Files.readAllBytes(new File(_href).toPath)
        val ctype = contentType.getOrElse("application/octet-stream")
        logger.debug(s"Loaded ${_href} as $ctype")
        consumer.get.receive("result", new ItemMessage(bytes, new XProcMetadata(ctype, docProps)))
    }
  }
}
