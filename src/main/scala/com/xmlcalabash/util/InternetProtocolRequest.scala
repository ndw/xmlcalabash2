package com.xmlcalabash.util

import com.jafpl.graph.Location
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XMLCalabashRuntime, XProcMetadata, XProcXPathExpression}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap, XdmValue}
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.{CookieStore, CredentialsProvider}
import org.apache.http.client.config.{AuthSchemes, CookieSpecs, RequestConfig}
import org.apache.http.client.methods.{HttpDelete, HttpGet, HttpHead, HttpPost, HttpPut, HttpRequestBase}
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.{BasicAuthCache, BasicCookieStore, BasicCredentialsProvider, HttpClientBuilder, StandardHttpRequestRetryHandler}
import org.apache.http.{Header, HttpHost, HttpResponse, ProtocolVersion}

import java.io.ByteArrayInputStream
import java.net.URI
import java.time.Instant
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

class InternetProtocolRequest(val config: XMLCalabashConfig, val context: StaticContext, val uri: URI) {
  private val _expires = new QName("", "expires")
  private val _date = new QName("", "date")
  private val _content_disposition = new QName("", "content-disposition")

  private var builder: HttpClientBuilder = _
  private var httpResult: HttpResponse = _
  private var href: URI = _
  private var finalURI: URI = _
  private val headers = mutable.HashMap.empty[String,String]

  private var _location = Option.empty[Location]
  private var _cookieStore = Option.empty[CookieStore]
  private var _timeout = Option.empty[Int]
  private val _sources = ListBuffer.empty[Array[Byte]] // serialization is your problem, not mine!
  private val _sourcesMetadata = ListBuffer.empty[XProcMetadata]
  private var _authMethod = Option.empty[String]
  private var _authPremptive = false
  private var _usercreds = Option.empty[UsernamePasswordCredentials]
  private var _httpVersion = Option.empty[Tuple2[Int,Int]]
  private var _statusOnly = false
  private var _overrideContentType = Option.empty[MediaType]
  private var _followRedirectCount = -1

  def this(config: XMLCalabashRuntime, context: StaticContext, uri: URI) =
    this(config.config, context, uri)

  def this(config: XMLCalabashConfig, uri: URI) =
    this(config, new StaticContext(config), uri)

  def location: Option[Location] = _location
  def location_=(loc: Location): Unit = {
    _location = Some(loc)
  }

  def cookieStore: Option[CookieStore] = _cookieStore
  def cookieStore_=(store: CookieStore): Unit = {
    // This is a mutable object, copy it
    _cookieStore = Some(new BasicCookieStore())
    for (cookie <- store.getCookies.asScala) {
      _cookieStore.get.addCookie(cookie)
    }
  }

  def timeout: Option[Int] = _timeout
  def timeout_=(timeout: Int): Unit = {
    _timeout = Some(timeout)
  }

  def httpVersion: Option[Tuple2[Int,Int]] = _httpVersion
  def httpVersion_=(version: Tuple2[Int,Int]): Unit = {
    _httpVersion = Some(version)
  }

  def statusOnly: Boolean = _statusOnly
  def statusOnly_=(only: Boolean): Unit = {
    _statusOnly = only
  }

  def overrideContentType: Option[MediaType] = _overrideContentType
  def overrideContentType_=(mtype: MediaType): Unit = {
    _overrideContentType = Some(mtype)
  }

  def followRedirectCount: Int = _followRedirectCount
  def followRedirectCount_=(count: Int): Unit = {
    _followRedirectCount = count
  }

  def addSource(item: Array[Byte], meta: XProcMetadata): Unit = {
    _sources += item;
    _sourcesMetadata += meta;
  }

  def addHeader(name: String, value: String): Unit = {
    if (name.equalsIgnoreCase("content-type")) {
      MediaType.parse(value).assertValid
    }
    headers.put(name, value);
  }

  def authentication(method: String, username: String, password: String): Unit = {
    authentication(method, username, password, false);
  }

  def authentication(method: String, username: String, password: String, premptive: Boolean): Unit = {
    if (method != "basic" && method != "digest") {
      throw XProcException.xcHttpBadAuth("auth-method must be 'basic' or 'digest'", location)
    }

    _authMethod = Some(method)
    _usercreds = Some(new UsernamePasswordCredentials(username, password))
    _authPremptive = premptive
  }

  def execute(method: String): InternetProtocolResponse = {
    href = uri
    builder = HttpClientBuilder.create()

    val rqbuilder = RequestConfig.custom()
    rqbuilder.setCookieSpec(CookieSpecs.DEFAULT)
    val localContext = HttpClientContext.create()

    if (cookieStore.isEmpty) {
      cookieStore = new BasicCookieStore()
    }

    builder.setDefaultCookieStore(cookieStore.get)

    if (timeout.isDefined) {
      rqbuilder.setSocketTimeout(timeout.get)
    }

    builder.setDefaultRequestConfig(rqbuilder.build())

    val httpRequest = method.toUpperCase() match {
      case "GET" =>
        setupGetOrHead(method.toUpperCase())
      case "POST" =>
        setupPutOrPost(method.toUpperCase())
      case "PUT" =>
        setupPutOrPost(method.toUpperCase())
      case "HEAD" =>
        setupGetOrHead(method.toUpperCase())
      case "DELETE" =>
        setupDelete()
      case _ =>
        throw new UnsupportedOperationException(s"Unsupported method: $method")
    }

    builder.setRetryHandler(new StandardHttpRequestRetryHandler(3, false))
    for (pscheme <- config.proxies.keySet) {
      val proxy = config.proxies(pscheme)
      val host = new HttpHost(proxy.getHost, proxy.getPort, pscheme)
      builder.setProxy(host)
    }

    if (_usercreds.isDefined) {
      val scope = new AuthScope(uri.getHost, uri.getPort)
      val bCredsProvider = new BasicCredentialsProvider()
      bCredsProvider.setCredentials(scope, _usercreds.get)
      var authpref: List[String] = List("")
      _authMethod.get match {
        case "basic" =>
          authpref = List(AuthSchemes.BASIC)
          if (_authPremptive) {
            // See https://stackoverflow.com/questions/20914311/httpclientbuilder-basic-auth
            val authCache = new BasicAuthCache()
            val basicAuth = new BasicScheme()
            authCache.put(new HttpHost(uri.getHost, uri.getPort), basicAuth)
            localContext.setCredentialsProvider(bCredsProvider)
            localContext.setAuthCache(authCache)
          }
        case "digest" =>
          authpref = List(AuthSchemes.DIGEST)
        case _ =>
          throw new RuntimeException("Unexpected authentication method: " + _authMethod.get)
      }

      rqbuilder.setProxyPreferredAuthSchemes(authpref.asJava)
      builder.setDefaultCredentialsProvider(bCredsProvider)
    }

    // FIXME: redirect is effectively boolean when it should be counting; will have to do by hand
    if (followRedirectCount == 0) {
      builder.disableRedirectHandling()
    }

    for ((header,value) <- headers) {
      httpRequest.setHeader(header, value)
    }

    val httpClient = builder.build()

    if (Option(httpClient).isEmpty) {
      throw new RuntimeException("HTTP requests have been disabled")
    }

    if (httpVersion.isDefined) {
      httpRequest.setProtocolVersion(new ProtocolVersion(href.getScheme, httpVersion.get._1, httpVersion.get._2))
    }

    httpResult = httpClient.execute(httpRequest, localContext)
    finalURI = httpRequest.getURI
    val locations = Option(localContext.getRedirectLocations)
    if (locations.isDefined) {
      finalURI = locations.get.get(locations.get.size() - 1)
    }

    val response = new InternetProtocolResponse(finalURI);
    response.statusCode = httpResult.getStatusLine.getStatusCode
    response.cookieStore = cookieStore.get
    response.report = requestReport()
    readResponseEntity(response)
  }

  private def readResponseEntity(response: InternetProtocolResponse): InternetProtocolResponse = {
    response.mediaType = getFullContentType

    if (Option(httpResult.getEntity).isEmpty) {
      val stream = new ByteArrayInputStream(Array.emptyByteArray)
      val meta = entityMetadata(httpResult.getAllHeaders.toList)
      response.addResponse(stream, meta)
      return response
    }

    if (statusOnly) {
      return response
    }

    if (response.mediaType.get.matches(MediaType.MULTIPART)) {
      readMultipartEntity(response)
    } else {
      readSinglepartEntity(response)
    }
  }

  private def readSinglepartEntity(response: InternetProtocolResponse): InternetProtocolResponse = {
    val stream = httpResult.getEntity.getContent
    val meta = entityMetadata(httpResult.getAllHeaders.toList)
    response.addResponse(stream, meta)
    response
  }

  private def readMultipartEntity(response: InternetProtocolResponse): InternetProtocolResponse = {
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
      // FIXME: properties
      val partURI = finalURI

      val meta = entityMetadata(reader.getHeaders)

      response.addResponse(partStream, meta)
    }

    response
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

  private def entityMetadata(headers: List[Header]): XProcMetadata = {
    var ctype = MediaType.OCTET_STREAM
    var location = false
    var baseURI = finalURI
    val props = mutable.HashMap.empty[QName, XdmValue]

    for (header <- headers) {
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

        if (key == XProcConstants._content_type) {
          if (overrideContentType.isDefined) {
            ctype = overrideContentType.get
            value = new XdmAtomicValue(overrideContentType.get.toString)
          } else {
            ctype = MediaType.parse(header.getValue).discardParams(List("charset"))
            value = new XdmAtomicValue(ctype.toString)
          }
        }

        if (key == _content_disposition) {
          for (helem <- header.getElements) {
            if (helem.getName == "attachment") {
              for (param <- helem.getParameters) {
                if (param.getName == "filename") {
                  baseURI = finalURI.resolve(param.getValue)
                }
              }
            }
          }
        }

        location = location ||  (key == XProcConstants._location)
        props.put(key, value)
      } catch {
        case _: Exception => ()
      }
    }

    if (location) {
      // Lie like a rug. An empty document might as well be text/plain as application/octet-stream
      props.put(XProcConstants._content_type, new XdmAtomicValue("text/plain"))
    }

    props.put(XProcConstants._base_uri, new XdmAtomicValue(baseURI))
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

    if (_sources.size == 1) {
      if (_sourcesMetadata.head.property("content-type").isDefined) {
        normHeaders.put("content-type", _sourcesMetadata.head.property("content-type").get.toString)
      }
    }

    for ((name,value) <- headers) {
      normHeaders.put(name.toLowerCase, value)
    }

    if (_sources.size == 1) {
      for ((name, value) <- _sourcesMetadata.head.properties) {
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

  private def setupGetOrHead(method: String): HttpRequestBase = {
    val request = if (method == "HEAD") {
      new HttpHead(href)
    } else {
      new HttpGet(href)
    }
    for ((name,value) <- normalizedHeaders) {
      request.addHeader(name, value)
    }
    request
  }

  private def setupPutOrPost(method: String): HttpRequestBase = {
    val normHeaders = normalizedHeaders
    val contentType = if (normHeaders.contains("content-type")) {
      if (_sources.size > 1 && !normHeaders("content-type").startsWith("multipart/")) {
        throw XProcException.xcMultipartRequired(normHeaders("content-type"), location)
      }
      normHeaders("content-type")
    } else {
      if (_sources.size > 1) {
        "multipart/mixed"
      } else {
        "application/octet-stream"
      }
    }

    if (contentType.startsWith("multipart/")) {
      return doMultipartPost()
    }

    val request = if (method == "POST") {
      new HttpPost(href)
    } else {
      new HttpPut(href)
    }

    for ((name,value) <- normHeaders) {
      request.addHeader(name, value)
    }

    if (_sources.nonEmpty) {
      request.setEntity(new ByteArrayEntity(_sources.head))
    }

    request
  }

  private def setupDelete(): HttpRequestBase = {
    // In theory, you can send content with a DELETE, but the underlying HTTP library doesn't
    // seem to support that.
    val request = new HttpDelete(href)
    for ((name,value) <- normalizedHeaders) {
      request.addHeader(name, value)
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
}
