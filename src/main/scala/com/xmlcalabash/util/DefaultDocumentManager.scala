package com.xmlcalabash.util

import java.io.{File, FileInputStream, IOException, InputStream}
import java.net.{URI, URLConnection}
import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.Date

import com.jafpl.messages.Message
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXSource
import com.xmlcalabash.config.{DocumentManager, DocumentRequest, DocumentResponse, XMLCalabash}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XProcMetadata, XProcXPathExpression}
import net.sf.saxon.s9api.{QName, SaxonApiException, XdmAtomicValue, XdmMap, XdmNode, XdmValue}
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.DateUtils
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.ByteArrayBuffer
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.helpers.XMLReaderFactory
import org.xml.sax.{InputSource, SAXException}

import scala.collection.immutable.HashMap
import scala.collection.mutable

class DefaultDocumentManager(xmlCalabash: XMLCalabash) extends DocumentManager {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def parse(request: DocumentRequest): DocumentResponse = {
    val baseURI = if (request.baseURI.isDefined) {
      request.baseURI.get
    } else {
      xmlCalabash.staticBaseURI
    }

    val ehref = URIUtils.encode(request.href)
    logger.trace("Attempting to parse: " + ehref + " (" + URIUtils.encode(baseURI) + ")")

    var href: Option[URI] = None
    val source = xmlCalabash.uriResolver.resolve(ehref, baseURI.toASCIIString)
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

      href = Some(resURI)
    } else {
      href = Some(new URI(source.getSystemId))
    }

    load(href.get, request)
  }

  private def load(href: URI, request: DocumentRequest): DocumentResponse = {
    href.getScheme match {
      case "file" => loadFile(href, request)
      case "http" => loadHttp(href, request)
      case "https" => loadHttp(href, request)
      case _ => throw new UnsupportedOperationException("Unexpected URI scheme: " + href.toASCIIString)
    }
  }

  private def loadFile(href: URI, request: DocumentRequest): DocumentResponse = {
    val contentType = computeContentType(href, request)
    val file = new File(href)
    val stream = new FileInputStream(file)
    request.baseURI = file.getAbsoluteFile.toURI

    val props = mutable.HashMap.empty[QName,XdmValue] ++ request.docprops
    props.put(XProcConstants._base_uri, new XdmAtomicValue(href.toASCIIString))
    props.put(XProcConstants._content_type, new XdmAtomicValue(contentType.toString))
    props.put(XProcConstants._content_length, new XdmAtomicValue(file.length()))

    val lastModified = new Date(file.lastModified())
    val zdt = ZonedDateTime.ofInstant(lastModified.toInstant, ZoneId.systemDefault())
    props.put(XProcConstants._last_modified, new XdmAtomicValue(zdt.format(DateTimeFormatter.ISO_INSTANT)))

    loadStream(request, contentType, stream, props.toMap)
  }

  private def loadHttp(href: URI, request: DocumentRequest): DocumentResponse = {
    val requestContentType = computeContentType(href, request)
    val httpclient = HttpClients.createDefault()
    var result: Option[DocumentResponse] = None

    try {
      val httpget = new HttpGet(href)

      val response = httpclient.execute(httpget)
      try {
        val responseEntity = Option(response.getEntity)
        if (responseEntity.isDefined) {
          val contentType = if (responseEntity.get.getContentType != null && responseEntity.get.getContentType.getValue != null) {
            MediaType.parse(responseEntity.get.getContentType.getValue)
          } else {
            requestContentType
          }

          val props = mutable.HashMap.empty[QName,XdmValue] ++ request.docprops
          props.put(XProcConstants._base_uri, new XdmAtomicValue(href.toASCIIString))
          props.put(XProcConstants._content_type, new XdmAtomicValue(contentType.toString))
          if (responseEntity.get.getContentLength >= 0) {
            props.put(XProcConstants._content_length, new XdmAtomicValue(responseEntity.get.getContentLength))
          }

          for (header <- response.getAllHeaders) {
            val name = header.getName.toLowerCase
            val qname = new QName("", "", name)
            name match {
              case "date" =>
                val date = DateUtils.parseDate(header.getValue)
                val zdt = ZonedDateTime.ofInstant(date.toInstant, ZoneId.systemDefault())
                props.put(qname, new XdmAtomicValue(zdt.format(DateTimeFormatter.ISO_INSTANT)))
              case "last-modified" =>
                val date = DateUtils.parseDate(header.getValue)
                val zdt = ZonedDateTime.ofInstant(date.toInstant, ZoneId.systemDefault)
                props.put(qname, new XdmAtomicValue(zdt.format(DateTimeFormatter.ISO_INSTANT)))
              case "content-length" => Unit
              case "content-type" => Unit
              case _ =>
                props.put(qname, new XdmAtomicValue(header.getValue))
            }
          }

          val stream = responseEntity.get.getContent
          try {
            result = Some(loadStream(request, contentType, stream, props.toMap))
          } catch {
            case ex: IOException => throw ex
          } finally {
            stream.close()
          }
        } else {
          throw new UnsupportedOperationException("No entity received?")
        }
      } finally {
        response.close()
      }
    } finally {
      httpclient.close()
    }

    if (result.isEmpty) {
      throw new IllegalArgumentException("Failed to load document")
    } else {
      result.get
    }
  }

  private def loadStream(request: DocumentRequest, contentType: MediaType, stream: InputStream, props: Map[QName,XdmValue]): DocumentResponse = {
    val value = if (contentType.htmlContentType) {
      parseHtml(request, new InputSource(stream)).value
    } else if (contentType.xmlContentType) {

      val source = new SAXSource(new InputSource(stream))
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

      val builder = xmlCalabash.processor.newDocumentBuilder
      builder.setDTDValidation(request.dtdValidate)
      builder.setLineNumbering(true)
      builder.build(source)
    } else if (contentType.jsonContentType) {
      val encoding = contentType.charset.getOrElse("UTF-8") // FIXME: What should the default be?
      val bytes = streamToByteArray(stream)

      val expr = new XProcXPathExpression(ExpressionContext.NONE, "parse-json($json)")
      val bindingsMap = mutable.HashMap.empty[String, Message]
      val vmsg = new XPathItemMessage(new XdmAtomicValue(new String(bytes, encoding)), XProcMetadata.JSON, ExpressionContext.NONE)
      bindingsMap.put("{}json", vmsg)
      val smsg = xmlCalabash.expressionEvaluator.singletonValue(expr, List(), bindingsMap.toMap, None)
      smsg.item
    } else if (contentType.textContentType) {
      val encoding = contentType.charset.getOrElse("UTF-8") // FIXME: What should the default be?
      val bytes = streamToByteArray(stream)

      val builder = new SaxonTreeBuilder(xmlCalabash)
      builder.startDocument(request.href)
      builder.addText(new String(bytes, encoding))
      builder.endDocument()
      builder.result
    } else {
      val bytes = streamToByteArray(stream)

      val builder = new SaxonTreeBuilder(xmlCalabash)
      builder.startDocument(request.href)
      builder.endDocument()
      builder.result
    }

    new DocumentResponse(value, contentType, props)
  }

  private def streamToByteArray(stream: InputStream): Array[Byte] = {
    val pagesize = 4096
    val buffer = new ByteArrayBuffer(pagesize)
    val tmp = new Array[Byte](4096)
    var length = 0
    length = stream.read(tmp)
    while (length >= 0) {
      buffer.append(tmp, 0, length)
      length = stream.read(tmp)
    }
    buffer.toByteArray
  }

  private def computeContentType(href: URI, request: DocumentRequest): MediaType = {
    // Using the filename sort of sucks, but it's what the OSes do at this point so...sigh
    // You can extend the set of known extensions by pointing the system property
    // `content.types.user.table` at your own mime types file. The default file to
    // start with is in $JAVA_HOME/lib/content-types.properties
    val map = URLConnection.getFileNameMap
    var contentTypeString = Option(URLConnection.guessContentTypeFromName(href.toASCIIString)).getOrElse("application/octet-stream")

    var propContentType = if (request.docprops.contains(XProcConstants._content_type)) {
      Some(MediaType.parse(request.docprops(XProcConstants._content_type).toString))
    } else {
      None
    }

    val contentType = if (propContentType.isDefined) {
      if (request.contentType.isDefined) {
        if (!request.contentType.get.matches(propContentType.get)) {
          throw XProcException.xdMismatchedContentType(request.contentType.get, propContentType.get, request.location)
        }
      }
      propContentType.get
    } else {
      if (request.contentType.isDefined) {
        request.contentType.get
      } else {
        MediaType.parse(contentTypeString)
      }
    }

    contentType
  }

  override def parse(request: DocumentRequest, isource: InputSource): DocumentResponse = {
    val node = try {
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

    new DocumentResponse(node, MediaType.XML, HashMap.empty[QName,XdmValue])
  }

  override def parseHtml(request: DocumentRequest): DocumentResponse = {
    val baseURI = if (request.baseURI.isDefined) {
      request.baseURI.get
    } else {
      xmlCalabash.staticBaseURI
    }
    val uri = baseURI.resolve(request.href)
    val src = new InputSource(uri.toASCIIString)
    parseHtml(request, src)
  }

  override def parseHtml(request: DocumentRequest, isource: InputSource): DocumentResponse = {
    val htmlBuilder = new HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET)
    htmlBuilder.setEntityResolver(xmlCalabash.entityResolver)
    val html = htmlBuilder.parse(isource)
    val builder = xmlCalabash.processor.newDocumentBuilder()
    new DocumentResponse(builder.build(new DOMSource(html)), MediaType.HTML, HashMap.empty[QName,XdmValue])
  }
}
