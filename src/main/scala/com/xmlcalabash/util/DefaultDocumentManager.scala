package com.xmlcalabash.util

import java.io.{File, FileInputStream, FileNotFoundException, IOException, InputStream, UnsupportedEncodingException}
import java.net.{URI, URLConnection}
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import com.jafpl.messages.Message
import com.xmlcalabash.config.{DocumentManager, DocumentRequest, DocumentResponse, XMLCalabashConfig}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, XProcMetadata, XProcXPathExpression}
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXSource
import net.sf.saxon.s9api.{QName, SaxonApiException, XdmAtomicValue, XdmValue}
import net.sf.saxon.trans.XPathException
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.DateUtils
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.ByteArrayBuffer
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.helpers.XMLReaderFactory
import org.xml.sax.{InputSource, SAXException}

import scala.collection.mutable
import scala.xml.SAXParseException

class DefaultDocumentManager(xmlCalabash: XMLCalabashConfig) extends DocumentManager {
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

    val response = load(href.get, request)
    response
  }

  override def parse(request: DocumentRequest, stream: InputStream): DocumentResponse = {
    loadStream(request, request.contentType.getOrElse(MediaType.OCTET_STREAM), stream, Map.empty[QName,XdmValue])
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
    try {
      val contentType = computeContentType(href, request)
      val file = new File(href)
      val stream = new FileInputStream(file)
      if (request.baseURI.isEmpty) {
        request.baseURI = file.getAbsoluteFile.toURI
      }

      val props = mutable.HashMap.empty[QName,XdmValue] ++ request.docprops
      if (!props.contains(XProcConstants._base_uri)) {
        props.put(XProcConstants._base_uri, new XdmAtomicValue(href.toASCIIString))
      }
      props.put(XProcConstants._content_type, new XdmAtomicValue(contentType.toString))
      props.put(XProcConstants._content_length, new XdmAtomicValue(file.length()))

      val lastModified = new Date(file.lastModified())
      val zdt = ZonedDateTime.ofInstant(lastModified.toInstant, ZoneId.systemDefault())
      props.put(XProcConstants._last_modified, new XdmAtomicValue(zdt.format(DateTimeFormatter.ISO_INSTANT)))

      loadStream(request, contentType, stream, props.toMap)
    } catch {
      case ex: FileNotFoundException =>
        throw XProcException.xdDoesNotExist(href.toASCIIString, request.location)
    }
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
          if (!props.contains(XProcConstants._base_uri)) {
            props.put(XProcConstants._base_uri, new XdmAtomicValue(href.toASCIIString))
          }
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
    } catch {
      case ex: Exception =>
        println(ex.getMessage)
        throw ex
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
    if (contentType.htmlContentType) {
      val value = parseHtml(request, new InputSource(stream)).value
      new DocumentResponse(value, contentType, props)
    } else if (contentType.xmlContentType) {
      val source = new SAXSource(new InputSource(stream))
      source.setSystemId(request.href.toASCIIString)
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

      // Is this necessary? I'm carefully synchronizing calls that mess with the
      // configuration's error listener.

      val listener = new CachingErrorListener()
      val saxonConfig = xmlCalabash.processor.getUnderlyingConfiguration
      saxonConfig.synchronized {
        listener.chainedListener = saxonConfig.getErrorListener
        saxonConfig.setErrorListener(listener)
      }

      val builder = xmlCalabash.processor.newDocumentBuilder
      builder.setDTDValidation(request.dtdValidate)
      builder.setLineNumbering(true)

      val node = try {
        builder.build(source)
      } catch {
        case sae: SaxonApiException =>
          val href = request.href.toASCIIString
          val msg = sae.getMessage
          if (msg.contains("validation")) {
            throw validationError(request, sae, listener.exceptions)
          } else if (msg.contains("HTTP response code: 403 ")) {
            throw XProcException.xdNotAuthorized(request.href.toASCIIString, msg)
          } else {
            // Let's try to do better about error locations.
            if (Option(sae.getCause).isDefined) {
              sae.getCause match {
                case ex: XPathException =>
                  val ns = ex.getErrorCodeNamespace
                  val code = ex.getErrorCodeLocalPart
                  ns match {
                    case XProcConstants.ns_xqt_errors =>
                      throw xqtError(code, request, sae.getCause)
                    case _ =>
                      throw XProcException.xdNotWFXML(href, msg, request.location)
                  }
                  throw XProcException.xdNotWFXML(href, msg, request.location)
                case _ =>
                  throw XProcException.xdNotWFXML(href, msg, request.location)
              }
            } else {
              throw XProcException.xdNotWFXML(href, msg, request.location)
            }
          }
      } finally {
        saxonConfig.synchronized {
          saxonConfig.setErrorListener(listener.chainedListener.get)
        }
      }

      new DocumentResponse(node, contentType, props)

    } else if (contentType.jsonContentType) {
      val encoding = contentType.charset.getOrElse("UTF-8") // FIXME: What should the default be?
      val bytes = streamToByteArray(stream)

      val expr = new XProcXPathExpression(ExpressionContext.NONE, "parse-json($json)")
      val bindingsMap = mutable.HashMap.empty[String, Message]
      val vmsg = new XdmValueItemMessage(new XdmAtomicValue(new String(bytes, encoding)), XProcMetadata.JSON, ExpressionContext.NONE)
      bindingsMap.put("{}json", vmsg)
      val smsg = xmlCalabash.expressionEvaluator.singletonValue(expr, List(), bindingsMap.toMap)
      new DocumentResponse(smsg.item, contentType, props)

    } else if (contentType.textContentType) {
      val encoding = contentType.charset.getOrElse("UTF-8") // FIXME: What should the default be?
      val bytes = streamToByteArray(stream)

      val builder = new SaxonTreeBuilder(xmlCalabash)
      builder.startDocument(request.href)

      try {
        builder.addText(new String(bytes, encoding))
      } catch {
        case ex: UnsupportedEncodingException =>
          throw XProcException.xdUnsupportedEncoding(encoding, request.location)
      }


      builder.endDocument()
      new DocumentResponse(builder.result, contentType, props)

    } else {
      val builder = new SaxonTreeBuilder(xmlCalabash)
      builder.startDocument(request.href)
      builder.endDocument()
      val result = builder.result
      new DocumentResponse(result, streamToByteArray(stream), contentType, props)
    }
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

  private def parseHtml(request: DocumentRequest): DocumentResponse = {
    val baseURI = if (request.baseURI.isDefined) {
      request.baseURI.get
    } else {
      xmlCalabash.staticBaseURI
    }
    val uri = baseURI.resolve(request.href)
    val src = new InputSource(uri.toASCIIString)
    parseHtml(request, src)
  }

  private def parseHtml(request: DocumentRequest, isource: InputSource): DocumentResponse = {
    val htmlBuilder = new HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET)
    htmlBuilder.setEntityResolver(xmlCalabash.entityResolver)
    val html = htmlBuilder.parse(isource)
    val builder = xmlCalabash.processor.newDocumentBuilder()
    new DocumentResponse(builder.build(new DOMSource(html)), MediaType.HTML, Map.empty[QName,XdmValue])
  }

  private def xqtError(str: String, request: DocumentRequest, cause: Throwable): XProcException = {
    val parseError = "org.xml.sax.SAXParseException; systemId: (.*); lineNumber: (\\d+); columnNumber: (\\d+); (.*)$".r

    val message = Option(cause.getMessage).getOrElse("Unknown error")
    val except = message match {
      case parseError(uri, line, col, msg) =>
        XProcException.xdNotWFXML(uri, line.toLong, col.toLong, msg, request.location)
      case _ =>
        XProcException.xdNotWFXML(request.href.toASCIIString, message, request.location)
    }

    cause match {
      case ex: Exception =>
        except.underlyingCauses = List(ex)
      case _ =>
        Unit
    }

    except
  }

  private def validationError(request: DocumentRequest, sae: SaxonApiException, exceptions: List[Exception]): XProcException = {
    val except = if (exceptions.isEmpty) {
      XProcException.xdNotValidXML(request.href.toASCIIString, sae.getMessage, request.location)
    } else {
      val err = exceptions.head
      err match {
        case xpex: XPathException =>
          val ex = xpex.getException
          ex match {
            case sxp: SAXParseException =>
              XProcException.xdNotValidXML(sxp.getSystemId, sxp.getLineNumber, sxp.getColumnNumber, sxp.getMessage, request.location)
            case _ =>
              XProcException.xdNotValidXML(request.href.toASCIIString, err.getMessage, request.location)
          }
        case _ =>
          XProcException.xdNotValidXML(request.href.toASCIIString, err.getMessage, request.location)
      }
    }

    except.underlyingCauses = exceptions
    except
  }
}
