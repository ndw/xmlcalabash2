package com.xmlcalabash.util

import java.net.URI

import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXSource
import com.xmlcalabash.config.{DocumentManager, XMLCalabash}
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.s9api.{SaxonApiException, XdmNode}
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.helpers.XMLReaderFactory
import org.xml.sax.{InputSource, SAXException}

class DefaultDocumentManager(xmlCalabash: XMLCalabash) extends DocumentManager {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def parse(uri: URI): XdmNode = {
    parse(uri, None)
  }

  override def parse(uri: URI, base: Option[URI]): XdmNode = {
    parse(uri, base, dtdValidate=false)
  }

  override def parse(href: URI, hbase: Option[URI], dtdValidate: Boolean): XdmNode = {
    val baseURI = if (hbase.isDefined) {
      hbase.get
    } else {
      xmlCalabash.staticBaseURI
    }

    val ehref = URIUtils.encode(href)
    logger.trace("Attempting to parse: " + ehref + " (" + URIUtils.encode(baseURI) + ")")

    var source = xmlCalabash.uriResolver.resolve(ehref, baseURI.toASCIIString)
    if (source == null) {
      var resURI = baseURI.resolve(ehref)
      val path = baseURI.toASCIIString
      val pos = path.indexOf("!")
      if (pos > 0 && (path.startsWith("jar:file:") || path.startsWith("jar:http:") || path.startsWith("jar:https:"))) {
        // You can't resolve() against jar: scheme URIs because they appear to be opaque.
        // I wonder if what follows is kosher...
        var fakeURIstr = "http://example.com"
        val subpath = path.substring(pos + 1)
        if (subpath.startsWith("/")) fakeURIstr += subpath
        else fakeURIstr += "/" + subpath
        val fakeURI = new URI(fakeURIstr)
        resURI = fakeURI.resolve(ehref)
        fakeURIstr = path.substring(0, pos + 1) + resURI.getPath
        resURI = new URI(fakeURIstr)
      }

      source = new SAXSource(new InputSource(resURI.toASCIIString))

      var reader = source.asInstanceOf[SAXSource].getXMLReader
      if (reader == null) {
        try {
          reader = XMLReaderFactory.createXMLReader
          source.asInstanceOf[SAXSource].setXMLReader(reader)
          reader.setEntityResolver(xmlCalabash.entityResolver)
        } catch {
          case se: SAXException => Unit
          case t: Throwable => throw t
        }
      }
    }

    val builder = xmlCalabash.processor.newDocumentBuilder
    builder.setDTDValidation(dtdValidate)
    builder.setLineNumbering(true)

    try
      builder.build(source)
    catch {
      case sae: SaxonApiException =>
        val msg = sae.getMessage
        if (msg.contains("validation")) {
          throw XProcException.xdNotValidXML(href.toASCIIString, msg)
        } else if (msg.contains("HTTP response code: 403 ")) {
          throw XProcException.xdNotAuthorized(href.toASCIIString, msg)
        } else {
          throw XProcException.xdNotWFXML(href.toASCIIString, msg)
        }
    }
  }

  override def parse(isource: InputSource): XdmNode = {
    try {
      // Make sure the builder uses our entity resolver
      val reader = XMLReaderFactory.createXMLReader
      reader.setEntityResolver(xmlCalabash.entityResolver)
      val source = new SAXSource(reader, isource)
      val builder = xmlCalabash.processor.newDocumentBuilder
      builder.setLineNumbering(true)
      builder.setDTDValidation(false)
      builder.build(source)
    } catch {
      case sae: SaxonApiException =>
        val msg = sae.getMessage
        if (msg.contains("validation")) {
          throw XProcException.xdNotValidXML(isource.getSystemId, msg)
        } else if (msg.contains("HTTP response code: 403 ")) {
          throw XProcException.xdNotAuthorized(isource.getSystemId, msg)
        } else {
          throw XProcException.xdNotWFXML(isource.getSystemId, msg)
        }
    }
  }

  override def parseHtml(uri: URI): XdmNode = {
    parseHtml(uri.toASCIIString, xmlCalabash.staticBaseURI.toASCIIString)
  }

  override def parseHtml(href: String): XdmNode = {
    parseHtml(href, xmlCalabash.staticBaseURI.toASCIIString)
  }

  override def parseHtml(href: String, base: String): XdmNode = {
    parseHtml(href, base, dtdValidate=false)
  }

  override def parseHtml(href: String, base: String, dtdValidate: Boolean): XdmNode = {
    val uri = new URI(base).resolve(href)
    val src = new InputSource(uri.toASCIIString)
    parseHtml(src)
  }

  override def parseHtml(isource: InputSource): XdmNode = {
    val htmlBuilder = new HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET)
    htmlBuilder.setEntityResolver(xmlCalabash.entityResolver)
    val html = htmlBuilder.parse(isource)
    val builder = xmlCalabash.processor.newDocumentBuilder()
    builder.build(new DOMSource(html))
  }
}
