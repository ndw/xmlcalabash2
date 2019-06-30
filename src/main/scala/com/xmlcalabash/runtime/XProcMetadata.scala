package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.messages.Metadata
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmValue}

import scala.collection.mutable

object XProcMetadata {
  private val _any = new XProcMetadata(MediaType.OCTET_STREAM)
  private val _xml = new XProcMetadata(MediaType.XML)
  private val _json = new XProcMetadata(MediaType.JSON)
  private val _html = new XProcMetadata(MediaType.HTML)
  private val _text = new XProcMetadata(MediaType.TEXT)
  private val _exception = new XProcMetadata(MediaType.OCTET_STREAM)
  def ANY: XProcMetadata = _any
  def XML: XProcMetadata = _xml
  def JSON: XProcMetadata = _json
  def HTML: XProcMetadata = _html
  def TEXT: XProcMetadata = _text
  def EXCEPTION: XProcMetadata = _exception
}

class XProcMetadata(private val initialContentType: Option[MediaType],
                    private val initialProperties: Map[QName,XdmValue]) extends Metadata {
  private val _properties = mutable.HashMap.empty[QName,XdmValue]
  private var _contentType: Option[MediaType] = None
  private var _baseURI: Option[URI] = None

  for ((key,value) <- initialProperties) {
    _properties.put(key, value)
  }

  if (initialContentType.isDefined) {
    _properties.put(XProcConstants._content_type, new XdmAtomicValue(initialContentType.get.toString))
  }

  def this(contentType: MediaType) {
    this(Some(contentType), Map.empty[QName,XdmValue])
  }
  def this(contentType: MediaType, initProp: Map[QName,XdmValue]) {
    this(Some(contentType), initProp)
  }
  def this(contentType: MediaType, metadata: XProcMetadata) {
    this(Some(contentType), metadata.properties)
  }

  def properties: Map[QName,XdmValue] = _properties.toMap
  def property(name: QName): Option[XdmValue] = {
    _properties.get(name)
  }
  def property(name: String): Option[XdmValue] = {
    _properties.get(new QName("", name))
  }

  def contentType: MediaType = {
    if (_contentType.isEmpty) {
      if (_properties.contains(XProcConstants._content_type)) {
        val value = _properties(XProcConstants._content_type)
        _contentType = Some(MediaType.parse(value.itemAt(0).getStringValue)) // FIXME: what about a sequence?
      } else {
        _contentType = Some(MediaType.OCTET_STREAM)
      }
    }

    _contentType.get
  }

  def baseURI: Option[URI] = {
    if (_baseURI.isEmpty) {
      if (_properties.contains(XProcConstants._base_uri)) {
        val value = _properties(XProcConstants._base_uri)
        _baseURI = Some(new URI(value.itemAt(0).getStringValue)) // FIXME: what about a sequence?
      }
    }

    _baseURI
  }
}
