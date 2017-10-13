package com.xmlcalabash.util

import java.net.URI
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXSource

import com.jafpl.exceptions.PipelineException
import com.xmlcalabash.config.{DocumentManager, XMLCalabash}
import net.sf.saxon.s9api.{SaxonApiException, XdmNode}
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.helpers.XMLReaderFactory
import org.xml.sax.{InputSource, SAXException}

class DefaultDocumentManager(xmlCalabash: XMLCalabash) extends DocumentManager {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def parse(uri: URI): XdmNode = {
    parse(uri.toASCIIString, xmlCalabash.staticBaseURI.toASCIIString)
  }

  override def parse(href: String): XdmNode = {
    parse(href, xmlCalabash.staticBaseURI.toASCIIString)
  }

  override def parse(href: String, base: String): XdmNode = {
    parse(href, base, dtdValidate=false)
  }

  override def parse(href: String, base: String, dtdValidate: Boolean): XdmNode = {
    val ehref = URIUtils.encode(href)
    logger.trace("Attempting to parse: " + ehref + " (" + base + ")")

    var source = xmlCalabash.uriResolver.resolve(ehref, base)
    if (source == null) {
      val baseURI = new URI(base)
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
          throw new PipelineException("invalid", "validation failed", None)
        } else if (msg.contains("HTTP response code: 403 ")) {
          throw new PipelineException("403", "xproc err 21", None)
        } else {
          throw new PipelineException("unk", "xproc err 11", None)
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
          throw new PipelineException("unk", "xproc err 27", None)
        } else if (msg.contains("HTTP response code: 403 ")) {
          throw new PipelineException("403", "xproc err 21", None)
        } else {
          throw new PipelineException("unk", "xproc err 11", None)
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
