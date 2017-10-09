package com.xmlcalabash.runtime

import com.jafpl.messages.Metadata
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem}

import scala.collection.mutable

object XProcMetadata {
  private val _any = new XProcMetadata("application/octet-stream")
  private val _xml = new XProcMetadata("application/xml")
  private val _exception = new XProcMetadata("application/octet-stream")
  def ANY = _any
  def XML = _xml
  def EXCEPTION = _exception
}

class XProcMetadata(private val initialContentType: Option[String],
                    private val initialProperties: Map[QName,XdmItem]) extends Metadata {
  private val _properties = mutable.HashMap.empty[QName,XdmItem]

  for ((key,value) <- initialProperties) {
    _properties.put(key, value)
  }

  if (initialContentType.isDefined) {
    _properties.put(XProcConstants._content_type, new XdmAtomicValue(initialContentType.get))
  }

  def this() {
    this(None, Map.empty[QName,XdmItem])
  }
  def this(contentType: String) {
    this(Some(contentType), Map.empty[QName,XdmItem])
  }
  def this(contentType: String, initProp: Map[QName,XdmItem]) {
    this(Some(contentType), initProp)
  }

  def properties: Map[QName,XdmItem] = _properties.toMap
  def property(name: QName): Option[XdmItem] = {
    _properties.get(name)
  }
  def property(name: String): Option[XdmItem] = {
    _properties.get(new QName("", name))
  }

  def contentType: String = {
    if (_properties.contains(XProcConstants._content_type)) {
      _properties(XProcConstants._content_type).getStringValue
    } else {
      "application/octet-stream"
    }
  }
}
