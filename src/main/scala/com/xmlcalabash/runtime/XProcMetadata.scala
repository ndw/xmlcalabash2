package com.xmlcalabash.runtime

import com.jafpl.messages.Metadata
import net.sf.saxon.s9api.XdmAtomicValue

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
                    private val initialProperties: Map[String,XdmAtomicValue]) extends Metadata {
  private val _properties = mutable.HashMap.empty[String,XdmAtomicValue]

  for ((key,value) <- initialProperties) {
    _properties.put(key, value)
  }

  if (initialContentType.isDefined && !_properties.contains("content-type")) {
    _properties.put("content-type", new XdmAtomicValue(initialContentType.get))
  }

  def this() {
    this(None, Map.empty[String,XdmAtomicValue])
  }
  def this(contentType: String) {
    this(Some(contentType), Map.empty[String,XdmAtomicValue])
  }
  def this(contentType: String, initProp: Map[String,XdmAtomicValue]) {
    this(Some(contentType), initProp)
  }

  def properties: Map[String,XdmAtomicValue] = _properties.toMap
  def property(name: String): Option[XdmAtomicValue] = {
    _properties.get(name)
  }
  def contentType: String = {
    if (_properties.contains("content-type")) {
      _properties("content-type").getStringValue
    } else {
      "application/octet-stream"
    }
  }
}
