package com.xmlcalabash.steps

import java.io.{InputStream, UnsupportedEncodingException}
import java.net.URI
import java.time.Instant
import java.time.format.{DateTimeFormatter, DateTimeParseException}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.jafpl.messages.Message
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.PortCardinality
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, StaticContext, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.util.xc.Errors
import com.xmlcalabash.util.{CachingErrorListener, MIMEReader, MediaType}
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXSource
import net.sf.saxon.s9api.{QName, SaxonApiException, XdmAtomicValue, XdmMap, XdmValue}
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import org.apache.http.client.config.{CookieSpecs, RequestConfig}
import org.apache.http.client.methods.{HttpGet, HttpPost, HttpUriRequest}
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.{HttpClientBuilder, StandardHttpRequestRetryHandler}
import org.apache.http.util.ByteArrayBuffer
import org.apache.http.{Consts, Header, HttpResponse}
import org.xml.sax.helpers.XMLReaderFactory
import org.xml.sax.{InputSource, SAXException}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class HttpRequest() extends DefaultXmlStep {
  private val _timeout = new QName("", "timeout")
  private val _expires = new QName("", "expires")
  private val _date = new QName("", "date")

  private val sources = ListBuffer.empty[Any]
  private val sourceMeta = ListBuffer.empty[XProcMetadata]

  private var context: StaticContext = _
  private var href: URI = _
  private var method = ""
  private var headers = mutable.HashMap.empty[String,String]
  private var auth = mutable.HashMap.empty[String, XdmValue]
  private var parameters = Map.empty[QName, XdmValue]
  private var assert = ""
  private var builder: HttpClientBuilder = _
  private var httpResult: HttpResponse = _
  private var finalURI: URI = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.ZERO_OR_MORE, "report" -> PortCardinality.EXACTLY_ONE),
    Map("result" -> List("application/octet-stream"),
      "report" -> List("application/json"))
  )

  override def initialize(config: RuntimeConfiguration): Unit = {
    super.initialize(config)
    builder = HttpClientBuilder.create()
  }

  override def reset(): Unit = {
    super.reset()
    href = null
    method = ""
    headers.clear()
    auth.clear()
    parameters = Map.empty[QName, XdmValue]
    assert = ""
    builder = HttpClientBuilder.create()
    httpResult = null
    finalURI = null
  }

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    sources += item
    sourceMeta += metadata
  }

  override def receiveBinding(variable: QName, value: XdmValue, context: StaticContext): Unit = {
    val _href = XProcConstants._href
    val _method = XProcConstants._method
    val _serialization = XProcConstants._serialization
    val _headers = new QName("", "headers")
    val _auth = new QName("", "auth")
    val _parameters = XProcConstants._parameters
    val _assert = new QName("", "assert")

    if (value.size() == 0) {
      return
    }

    variable match {
      case `_href` =>
        href = if (context.baseURI.isDefined) {
          context.baseURI.get.resolve(value.getUnderlyingValue.getStringValue)
        } else {
          new URI(value.getUnderlyingValue.getStringValue)
        }
      case `_method` =>
        method = value.getUnderlyingValue.getStringValue.toUpperCase
      case `_serialization` =>
        // nop; this his handled elsewhere
      case `_headers` =>
        value match {
          case map: XdmMap =>
            // Grovel through a Java Map
            val iter = map.keySet().iterator()
            while (iter.hasNext) {
              val key = iter.next()
              val value = map.get(key)
              headers.put(key.getStringValue, value.getUnderlyingValue.getStringValue)
            }
          case _ =>
            throw new IllegalArgumentException("Headers is not a map(xs:string,xs:string)")
        }
      case `_auth` =>
        value match {
          case map: XdmMap =>
            // Grovel through a Java Map
            val iter = map.keySet().iterator()
            while (iter.hasNext) {
              val key = iter.next()
              val value = map.get(key)
              auth.put(key.getStringValue, value)
            }
          case _ =>
            throw new IllegalArgumentException("Headers is not a map(xs:string,xs:string)")
        }
      case `_parameters` =>
        parameters = ValueParser.parseParameters(value, context)
      case `_assert` =>
        assert = value.getUnderlyingValue.getStringValue
      case _ => Unit
    }
  }

  override def run(ctx: StaticContext): Unit = {
    context = ctx

    if (href.getScheme == "file") {
      doFile()
    } else if (href.getScheme == "http" || href.getScheme == "https") {
      doHttp()
    } else {
      throw new RuntimeException("Unsupported URI scheme: " + href.toASCIIString)
    }
  }

  private def doHttp(): Unit = {
    val rqbuilder = RequestConfig.custom()
    rqbuilder.setCookieSpec(CookieSpecs.DEFAULT)
    val localContext = HttpClientContext.create()

    // FIXME: deal with cookies

    if (parameters.contains(_timeout)) {
      rqbuilder.setSocketTimeout(Integer.parseInt(parameters(_timeout).getUnderlyingValue.getStringValue))
    }

    builder.setDefaultRequestConfig(rqbuilder.build())

    // FIXME: deal with auth

    // FIXME: deal with source

    val httpRequest = method match {
      case "GET" =>
        doGet()
      case "POST" =>
        doPost()
      case _ =>
        throw new UnsupportedOperationException(s"Unsupported method: $method")
    }

    builder.setRetryHandler(new StandardHttpRequestRetryHandler(3, false))
    // FIXME: deal with proxy

    val httpClient = builder.build()
    if (Option(httpClient).isEmpty) {
      throw new RuntimeException("HTTP requests have been disabled")
    }

    httpResult = httpClient.execute(httpRequest, localContext)
    finalURI = httpRequest.getURI
    val locations = Option(localContext.getRedirectLocations)
    if (locations.isDefined) {
      finalURI = locations.get.get(locations.get.size() - 1)
    }

    val report = requestReport()

    if (assert != "") {
      val msg = new XdmValueItemMessage(report, XProcMetadata.JSON, context)
      val expr = new XProcXPathExpression(context, assert)
      val ok = config.expressionEvaluator.booleanValue(expr, List(msg), Map.empty[String,Message], None)
      if (!ok) {
        throw XProcException.xcHttpAssertFailed(assert, location)
      }
    }

    consumer.get.receive("report", report, new XProcMetadata(MediaType.JSON))

    readResponseEntity()
  }

  private def readResponseEntity(): Unit = {
    if (Option(httpResult.getEntity).isEmpty) {
      return
    }

    val contentType = getFullContentType
    if (contentType.matches(MediaType.MULTIPART)) {
      readMultipartEntity()
    } else {
      readSinglepartEntity()
    }
  }

  private def readSinglepartEntity(): Unit = {
    val stream = httpResult.getEntity.getContent
    val contentType = getFullContentType
    val request = new DocumentRequest(finalURI, contentType, location)
    val result = config.documentManager.parse(request, stream)

    val meta = entityMetadata()

    if (result.shadow.isDefined) {
      val node = new BinaryNode(config, result.shadow.get)
      consumer.get.receive("result", node, meta)
    } else {
      consumer.get.receive("result", result.value, meta)
    }
  }

  private def readMultipartEntity(): Unit = {
    val contentType = getFullContentType
    val boundary = contentType.paramValue("boundary")

    val reader = new MIMEReader(httpResult.getEntity.getContent, boundary.get)
    while (reader.readHeaders()) {
      val pctype = reader.header("Content-Type")
      val pclen = reader.header("Content-Length")

      val contentType = MediaType.parse(getHeaderValue(pctype).getOrElse("application/octet-stream"))

      val partStream = if (pclen.isDefined) {
        val len = getHeaderValue(pclen).get.toLong
        reader.readBodyPart(len)
      } else {
        reader.readBodyPart()
      }

      // FIXME: handle content-disposition: attachment; filename="something.xml"
      val partURI = finalURI

      val request = new DocumentRequest(partURI, contentType)
      request.baseURI = partURI

      val result = config.documentManager.parse(request, partStream)

      if (result.shadow.isDefined) {
        val node = new BinaryNode(config, result.shadow.get)
        consumer.get.receive("result", node, new XProcMetadata(result.contentType, result.props))
      } else {
        consumer.get.receive("result", result.value, new XProcMetadata(result.contentType, result.props))
      }
    }
  }

  private def getHeaderValue(header: Option[Header]): Option[String] = {
    if (header.isEmpty) {
      None
    } else {
      val elems = header.get.getElements
      if (elems == null || elems.isEmpty) {
        None
      } else {
        Some(elems(0).toString)
      }
    }
  }

  private def getFullContentType: MediaType = {
    val ctype = httpResult.getLastHeader("Content-Type")
    if (Option(ctype).isEmpty) {
      return MediaType.OCTET_STREAM
    }

    val types = ctype.getElements
    if (Option(types).isEmpty || types.isEmpty) {
      return MediaType.OCTET_STREAM
    }

    var params = ""
    for (param <- types(0).getParameters) {
      params += "; "
      params += param.getName.toLowerCase() + "=" + param.getValue
    }

    MediaType.parse(types(0).getName + params)
  }

  private def entityMetadata(): XProcMetadata = {
    var ctype = MediaType.OCTET_STREAM
    var props = mutable.HashMap.empty[QName, XdmValue]

    props.put(XProcConstants._base_uri, new XdmAtomicValue(finalURI))

    for (header <- httpResult.getAllHeaders) {
      try {
        val key = new QName("", header.getName.toLowerCase)
        var value = new XdmAtomicValue(header.getValue)

        if (key == _date || key == _expires) {
          try {
            // Convert date time strings into proper xs:dateTime values
            val ta = DateTimeFormatter.RFC_1123_DATE_TIME.parse(header.getValue)
            val dt = Instant.from(ta).toString
            val expr = new XProcXPathExpression(context, s"xs:dateTime('$dt')")
            val smsg = config.expressionEvaluator.singletonValue(expr, List(), Map(), None)
            value = smsg.item.asInstanceOf[XdmAtomicValue]
          } catch {
            case _: DateTimeParseException => Unit
          }
        }

        if (key == XProcConstants._content_type) {
          ctype = MediaType.parse(header.getValue).discardParams(List("charset"))
          value = new XdmAtomicValue(ctype.toString)
        }

        props.put(key, value)
      } catch {
        case _: Exception => Unit
      }
    }

    new XProcMetadata(ctype, props.toMap)
  }

  private def requestReport(): XdmMap = {
    var report = new XdmMap()
    report = report.put(new XdmAtomicValue("status-code"), new XdmAtomicValue(httpResult.getStatusLine.getStatusCode))

    var headers = new XdmMap()
    for (header <- httpResult.getAllHeaders) {
      val key = header.getName.toLowerCase
      val value = header.getValue
      headers = headers.put(new XdmAtomicValue(key), new XdmAtomicValue(value))
    }

    report.put(new XdmAtomicValue("headers"), headers)
  }

  private def doGet(): HttpUriRequest = {
    val method = new HttpGet(href)
    // FIXME: deal with headers
    method
  }

  private def doPost(): HttpUriRequest = {
    val method = new HttpPost(href)
    // FIXME: deal with headers

    method.addHeader("content-type", "application/xml")
    method.setEntity(new StringEntity("<doc/>"))

    method
  }

  private def doFile(): Unit = {
    throw new RuntimeException("Unsupported URI scheme: " + href.toASCIIString)
  }
}
