package com.xmlcalabash.runtime

import com.jafpl.messages.Metadata

import scala.collection.mutable

object XProcMetadata {
  private val _any = new XProcMetadata("application/octet-stream")
  def ANY = _any
}

class XProcMetadata(private val initialContentType: Option[String],
                    private val initialProperties: Map[String,String]) extends Metadata {
  private val _properties = mutable.HashMap.empty[String,String]

  for ((key,value) <- initialProperties) {
    _properties.put(key, value)
  }

  if (initialContentType.isDefined && !_properties.contains("content-type")) {
    _properties.put("content-type", initialContentType.get)
  }

  def this() {
    this(None, Map.empty[String,String])
  }
  def this(contentType: String) {
    this(Some(contentType), Map.empty[String,String])
  }
  def this(contentType: String, initProp: Map[String,String]) {
    this(Some(contentType), initProp)
  }

  def properties: Map[String,String] = _properties.toMap
  def property(name: String): Option[String] = {
    _properties.get(name)
  }
  def contentType: String = {
    _properties.getOrElse("content-type", "application/octet-stream")
  }
}
