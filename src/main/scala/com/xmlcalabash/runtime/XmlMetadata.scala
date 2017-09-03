package com.xmlcalabash.runtime

import com.jafpl.messages.Metadata

import scala.collection.mutable

class XmlMetadata(private val initialContentType: Option[String],
                  private val initialProperties: Option[Map[String,String]]) extends Metadata {
  private val _properties = mutable.HashMap.empty[String,String]

  if (initialProperties.isDefined) {
    for ((key,value) <- initialProperties.get) {
      _properties.put(key, value)
    }
  }

  if (initialContentType.isDefined && !_properties.contains("content-type")) {
    _properties.put("content-type", initialContentType.get)
  }

  def this() {
    this(None, None)
  }
  def this(contentType: String) {
    this(Some(contentType), None)
  }
  def this(contentType: String, initProp: Map[String,String]) {
    this(Some(contentType), Some(initProp))
  }

  def properties: Map[String,String] = _properties.toMap
  def property(name: String): Option[String] = {
    _properties.get(name)
  }
  def contentType: String = {
    _properties.getOrElse("content-type", "application/octet-stream")
  }
}
