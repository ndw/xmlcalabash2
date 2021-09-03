package com.xmlcalabash.test

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcMetadata}
import com.xmlcalabash.util.{InternetProtocolRequest, MediaType}
import org.scalatest.flatspec.AnyFlatSpec

import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets

class InternetProtocolRequestSpec extends AnyFlatSpec {
  private val config = XMLCalabashConfig.newInstance()

  // =============================================

  "GET " should " return a document" in {
    val request = new InternetProtocolRequest(config)
    val response = request.httpRequest("GET", URI.create("http://localhost:8246/service/fixed-xml"))
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 200)
  }

  "GET " should " return a multipart document" in {
    val request = new InternetProtocolRequest(config)
    val response = request.httpRequest("GET", URI.create("http://localhost:8246/service/fixed-multipart"))
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 200)
  }

  "GET " should " return the file download URI" in {
    val request = new InternetProtocolRequest(config)
    val response = request.httpRequest("GET", URI.create("http://localhost:8246/service/file-download"))
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 200)
    assert(response.finalURI == URI.create("http://localhost:8246/service/download-test.xml"))
  }

  "GET " should " return URIs for multipart attachments" in {
    val request = new InternetProtocolRequest(config)
    val response = request.httpRequest("GET", URI.create("http://localhost:8246/service/file-multidownload"))
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 200)
    assert(response.finalURI == URI.create("http://localhost:8246/service/file-multidownload"))
    assert(response.response.length == 2)
    assert(response.responseMetadata.length == 2)
    assert(response.responseMetadata.head.baseURI.get == URI.create("http://localhost:8246/service/download-part1.html"))
    assert(response.responseMetadata(1).baseURI.get == URI.create("http://localhost:8246/service/images/download-part2.png"))
  }

  "POST " should " succeed" in {
    val request = new InternetProtocolRequest(config)
    val meta = new XProcMetadata(MediaType.parse("text/plain"))
    request.addSource("Hello, world.".getBytes(StandardCharsets.UTF_8), meta)
    val response = request.httpRequest("POST", URI.create("http://localhost:8246/service/accept-post"))
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 201)
  }

  "PUT " should " succeed" in {
    val request = new InternetProtocolRequest(config)
    val meta = new XProcMetadata(MediaType.parse("text/plain"))
    request.addSource("Hello, world.".getBytes(StandardCharsets.UTF_8), meta)
    val response = request.httpRequest("PUT", URI.create("http://localhost:8246/service/accept-put"))
    assert(response != null)
    assert(response.statusCode.isDefined)
    assert(response.statusCode.get == 202)
  }
}
