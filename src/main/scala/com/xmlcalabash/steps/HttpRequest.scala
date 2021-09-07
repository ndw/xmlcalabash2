package com.xmlcalabash.steps

import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.Instant
import java.time.format.{DateTimeFormatter, DateTimeParseException}

import com.jafpl.messages.Message
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.PortCardinality
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, StaticContext, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.util.{MIMEReader, MediaType}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap, XdmValue}
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.config.{CookieSpecs, RequestConfig}
import org.apache.http.client.methods.{HttpGet, HttpPost, HttpRequestBase}
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.{BasicCredentialsProvider, HttpClientBuilder, StandardHttpRequestRetryHandler}
import org.apache.http.{Header, HttpResponse, ProtocolVersion}

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
  private val headers = mutable.HashMap.empty[String,String]
  private val auth = mutable.HashMap.empty[String, XdmValue]
  private val parameters = mutable.HashMap.empty[QName, XdmValue]
  private var assert = ""
  private var builder: HttpClientBuilder = _
  private var httpResult: HttpResponse = _
  private var finalURI: URI = _

  // Parameters
  private var httpVersion = Option.empty[ProtocolVersion]
  private var overrideContentType = Option.empty[MediaType]
  private var acceptMultipart = true
  private var overrideContentEncoding = Option.empty[String]
  private var permitExpiredSslCertificate = false
  private var permitUntrustedSslCertificate = false
  private var followRedirectCount = -1
  private var timeout = 0
  private var failOnTimeout = false
  private var statusOnly = false
  private var suppressCookies = false
  private var sendBodyAnyway = false

  // Authentication
  private var username = Option.empty[String]
  private var password = Option.empty[String]
  private var authmethod = Option.empty[String]
  private var sendauth = false
  private var usercreds = Option.empty[UsernamePasswordCredentials]

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
    parameters.clear()
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
        parameters ++= ValueParser.parseParameters(value, context)
      case `_assert` =>
        assert = value.getUnderlyingValue.getStringValue
      case _ => ()
    }
  }

  override def run(ctx: StaticContext): Unit = {
    context = ctx

    // Check parameters
    for ((name, value) <- parameters) {
      name match {
        case XProcConstants._http_version =>
          parameterHttpVersion(value)
        case XProcConstants._override_content_type =>
          parameterOverrideContentType(value)
        case XProcConstants._accept_multipart =>
          acceptMultipart = booleanParameter(name.getLocalName, value)
        case XProcConstants._timeout =>
          timeout = integerParameter(name.getLocalName, value)
          if (timeout < 0) {
            throw XProcException.xcHttpInvalidParameter(name.getLocalName, value.toString, location)
          }
        case XProcConstants._permit_expired_ssl_certificate =>
          permitExpiredSslCertificate = booleanParameter(name.getLocalName, value)
        case XProcConstants._permit_untrusted_ssl_certificate =>
          permitUntrustedSslCertificate = booleanParameter(name.getLocalName, value)
        case XProcConstants._override_content_encoding =>
          overrideContentEncoding = Some(stringParameter(name.getLocalName, value))
        case XProcConstants._follow_redirect =>
          followRedirectCount = integerParameter(name.getLocalName, value)
        case XProcConstants._fail_on_timeout =>
          failOnTimeout = booleanParameter(name.getLocalName, value)
        case XProcConstants._status_only =>
          statusOnly = booleanParameter(name.getLocalName, value)
        case XProcConstants._suppress_cookies =>
          suppressCookies = booleanParameter(name.getLocalName, value)
        case XProcConstants._send_body_anyway =>
          sendBodyAnyway = booleanParameter(name.getLocalName, value)
        case _ =>
          logger.debug(s"Unexpected http-request parameter: ${name.getLocalName}")
      }
    }

    // Check auth
    for ((name, value) <- auth) {
      name match {
        case "username" =>
          username = Some(stringAuth(name, value))
        case "password" =>
          password = Some(stringAuth(name, value))
        case "auth-method" =>
          authmethod = Some(stringAuth(name, value).toLowerCase)
        case "send-authorization" =>
          sendauth = booleanAuth(name, value)
        case _ =>
          logger.debug(s"Unexpected http-request authentication parameter: $name")
      }
    }

    if (password.isDefined && username.isEmpty) {
      throw XProcException.xcHttpBadAuth("username must be specified if password is specified", location)
    }

    if (username.isDefined) {
      if (password.isEmpty) {
        password = Some("")
      }
      usercreds = Some(new UsernamePasswordCredentials(username.get, password.get))
    }

    if (username.isDefined && authmethod.isEmpty) {
      throw XProcException.xcHttpBadAuth("auth-method must be specified", location)
    }

    if (authmethod.isDefined) {
      if (authmethod.get != "basic" && authmethod.get != "digest") {
        throw XProcException.xcHttpBadAuth("auth-method must be 'basic' or 'digest'", location)
      }
    }

    if (href.getScheme == "file") {
      doFile()
    } else if (href.getScheme == "http" || href.getScheme == "https") {
      doHttp()
    } else {
      throw new RuntimeException("Unsupported URI scheme: " + href.toASCIIString)
    }
  }

  private def parameterOverrideContentType(value: XdmValue): Unit = {
    try {
      overrideContentType = Some(MediaType.parse(value.toString))
    } catch {
      case _: Exception =>
        throw XProcException.xcHttpInvalidParameter("override-content-type", value.toString, location)
    }
  }

  private def parameterHttpVersion(value: XdmValue): Unit = {
    if (!value.isInstanceOf[XdmAtomicValue]) {
      throw XProcException.xcHttpInvalidParameter("http-version", value.toString, location)
    }

    val atomicValue = value.asInstanceOf[XdmAtomicValue]
    val vtype = atomicValue.getTypeName
    if (vtype != XProcConstants.xs_string) {
      throw XProcException.xcHttpInvalidParameter("http-version", value.toString, location)
    }

    var majVer = 0
    var minVer = 0

    try {
      val double = value.toString.toDouble // Ok, it's a number
      var str = value.toString
      if (str.indexOf(".") < 0) {
        str += ".0"
      }
      val pos = str.indexOf(".")
      majVer = str.substring(0, pos).toInt
      minVer = str.substring(pos+1).toInt
    } catch {
      case _: Exception =>
        throw XProcException.xcHttpInvalidParameter("http-version", value.toString, location)
    }

    httpVersion = Some(new ProtocolVersion(href.getScheme, minVer, majVer))
  }

  private def booleanParameter(name: String, value: XdmValue): Boolean = {
    if (!value.isInstanceOf[XdmAtomicValue]) {
      throw XProcException.xcHttpInvalidParameter(name, value.toString, location)
    }

    val atomicValue = value.asInstanceOf[XdmAtomicValue]
    val vtype = atomicValue.getTypeName
    if (vtype != XProcConstants.xs_boolean) {
      throw XProcException.xcHttpInvalidParameter(name, value.toString, location)
    }

    atomicValue.getBooleanValue
  }

  private def integerParameter(name: String, value: XdmValue): Int = {
    if (!value.isInstanceOf[XdmAtomicValue]) {
      throw XProcException.xcHttpInvalidParameter(name, value.toString, location)
    }

    val atomicValue = value.asInstanceOf[XdmAtomicValue]
    val vtype = atomicValue.getTypeName
    if (vtype != XProcConstants.xs_integer) {
      throw XProcException.xcHttpInvalidParameter(name, value.toString, location)
    }

    atomicValue.toString.toInt
  }

  private def stringParameter(name: String, value: XdmValue): String = {
    if (!value.isInstanceOf[XdmAtomicValue]) {
      throw XProcException.xcHttpInvalidParameter(name, value.toString, location)
    }

    val atomicValue = value.asInstanceOf[XdmAtomicValue]
    val vtype = atomicValue.getTypeName
    if (vtype != XProcConstants.xs_string) {
      throw XProcException.xcHttpInvalidParameter(name, value.toString, location)
    }

    atomicValue.toString
  }

  private def stringAuth(name: String, value: XdmValue): String = {
    if (!value.isInstanceOf[XdmAtomicValue]) {
      throw XProcException.xcHttpInvalidAuth(name, value.toString, location)
    }

    val atomicValue = value.asInstanceOf[XdmAtomicValue]
    val vtype = atomicValue.getTypeName
    if (vtype != XProcConstants.xs_string) {
      throw XProcException.xcHttpInvalidAuth(name, value.toString, location)
    }

    atomicValue.toString
  }

  private def booleanAuth(name: String, value: XdmValue): Boolean = {
    if (!value.isInstanceOf[XdmAtomicValue]) {
      throw XProcException.xcHttpInvalidAuth(name, value.toString, location)
    }

    val atomicValue = value.asInstanceOf[XdmAtomicValue]
    val vtype = atomicValue.getTypeName
    if (vtype != XProcConstants.xs_boolean) {
      throw XProcException.xcHttpInvalidAuth(name, value.toString, location)
    }

    atomicValue.getBooleanValue
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

    var provider = Option.empty[CredentialsProvider]
    if (usercreds.isDefined) {
      if (authmethod.get == "basic") {
        val basic = new BasicCredentialsProvider()
        basic.setCredentials(AuthScope.ANY, usercreds.get)
        provider = Some(basic)
      } else { // digest
        throw new UnsupportedOperationException("Can't handle digest auth yet")
      }
    }

    if (provider.isDefined) {
      builder.setDefaultCredentialsProvider(provider.get)
    }

    // FIXME: redirect is effectively boolean when it should be counting; will have to do by hand
    if (followRedirectCount == 0) {
      builder.disableRedirectHandling()
    }

    val httpClient = builder.build()

    if (Option(httpClient).isEmpty) {
      throw new RuntimeException("HTTP requests have been disabled")
    }

    if (httpVersion.isDefined) {
      httpRequest.setProtocolVersion(httpVersion.get)
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
      val exeval = config.expressionEvaluator.newInstance()
      val ok = exeval.booleanValue(expr, List(msg), Map.empty[String,Message], None)
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
      if (!acceptMultipart) {
        throw XProcException.xcHttpMultipartForbidden(location)
      }
      if (!statusOnly) {
        readMultipartEntity()
      }
    } else {
      if (!statusOnly) {
        readSinglepartEntity()
      }
    }
  }

  private def readSinglepartEntity(): Unit = {
    val stream = httpResult.getEntity.getContent
    val contentType = getFullContentType
    val request = new DocumentRequest(finalURI, contentType, location)
    val result = try {
      config.documentManager.parse(request, stream)
    } catch {
      case ex: Exception =>
        if (overrideContentType.isDefined) {
          throw XProcException.xcHttpCantInterpret(ex.getMessage, location)
        } else {
          throw ex
        }
    }

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
    if (overrideContentType.isDefined) {
      return overrideContentType.get
    }

    val ctype = httpResult.getLastHeader("Content-Type")
    if (Option(ctype).isEmpty) {
      if (httpResult.getLastHeader("Location") == null) {
        return MediaType.OCTET_STREAM
      } else {
        // This is kind of a lie, but it's a safe one
        return MediaType.TEXT
      }
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
    var ctype = getFullContentType
    val props = mutable.HashMap.empty[QName, XdmValue]

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
            val smsg = config.expressionEvaluator.newInstance().singletonValue(expr, List(), Map(), None)
            value = smsg.item.asInstanceOf[XdmAtomicValue]
          } catch {
            case _: DateTimeParseException => ()
          }
        }

        props.put(key, value)
      } catch {
        case _: Exception => ()
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

  private def normalizedHeaders: Map[String,String] = {
    val normHeaders = mutable.HashMap.empty[String,String]

    for ((name,value) <- headers) {
      if (normHeaders.contains(name.toLowerCase)) {
        throw XProcException.xcHttpDuplicateHeader(name.toLowerCase, location)
      }
      normHeaders.put(name.toLowerCase, value)
    }
    normHeaders.clear()

    if (sources.size == 1) {
      if (sourceMeta.head.property("content-type").isDefined) {
        normHeaders.put("content-type", sourceMeta.head.property("content-type").get.toString)
      }
    }

    for ((name,value) <- headers) {
      normHeaders.put(name.toLowerCase, value)
    }

    if (sources.size == 1) {
      for ((name, value) <- sourceMeta.head.properties) {
        if (name.getNamespaceURI == XProcConstants.ns_chttp) {
          val key = name.getLocalName.toLowerCase
          if (!normHeaders.contains(key)) {
            normHeaders.put(name.getLocalName, value.toString)
          }
        }
      }
    }

    normHeaders.toMap
  }

  private def doGet(): HttpRequestBase = {
    val request = new HttpGet(href)
    for ((name,value) <- normalizedHeaders) {
      request.addHeader(name, value)
    }
    request
  }

  private def doPost(): HttpRequestBase = {
    val normHeaders = normalizedHeaders
    val contentType = if (normHeaders.contains("content-type")) {
      if (sources.size > 1 && !normHeaders("content-type").startsWith("multipart/")) {
        throw XProcException.xcMultipartRequired(normHeaders("content-type"), location)
      }
      normHeaders("content-type")
    } else {
      if (sources.size > 1) {
        "multipart/mixed"
      } else {
        "application/octet-stream"
      }
    }

    if (contentType.startsWith("multipart/")) {
      return doMultipartPost()
    }

    val request = new HttpPost(href)
    for ((name,value) <- normHeaders) {
      request.addHeader(name, value)
    }

    if (sources.nonEmpty) {
      val os = new ByteArrayOutputStream()
      serialize(context, sources.head, sourceMeta.head, os)
      os.close()
      request.setEntity(new ByteArrayEntity(os.toByteArray))
    }

    request
  }

  private def doMultipartPost(): HttpRequestBase = {
    val request = new HttpPost(href)
    for ((name,value) <- normalizedHeaders) {
      request.addHeader(name, value)
    }
    throw new UnsupportedOperationException("Multipart post is not implemented yet")
  }

  private def doFile(): Unit = {
    throw new UnsupportedOperationException("Unsupported URI scheme: " + href.toASCIIString)
  }
}
