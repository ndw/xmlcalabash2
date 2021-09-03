package com.xmlcalabash.util

import com.xmlcalabash.runtime.XProcMetadata
import net.sf.saxon.s9api.XdmMap

import java.io.InputStream
import java.net.URI
import scala.collection.mutable.ListBuffer

class InternetProtocolResponse(val finalURI: URI) {
  private var _statusCode = Option.empty[Int]
  private var _report = Option.empty[XdmMap]
  private var _mediaType = Option.empty[MediaType]
  private val _response = ListBuffer.empty[InputStream]
  private val _responseMetadata = ListBuffer.empty[XProcMetadata]

  def statusCode: Option[Int] = _statusCode

  def statusCode_=(value: Int): Unit = {
    _statusCode = Some(value)
  }

  def report: Option[XdmMap] = _report

  def report_=(map: XdmMap): Unit = {
    _report = Some(map)
  }

  def mediaType: Option[MediaType] = _mediaType

  def mediaType_=(mtype: MediaType): Unit = {
    _mediaType = Some(mtype)
  }

  def addResponse(response: InputStream, meta: XProcMetadata): Unit = {
    _response += response
    _responseMetadata += meta
  }

  def empty: Boolean = _response.isEmpty

  def singlepart: Boolean = !multipart

  def multipart: Boolean = _response.length > 1

  def response: ListBuffer[InputStream] = _response

  def responseMetadata: ListBuffer[XProcMetadata] = _responseMetadata
}
