package com.xmlcalabash.config

import java.net.URI

import com.jafpl.graph.Location
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmValue}

import scala.collection.immutable.HashMap

class DocumentRequest(val href: URI, val contentType: Option[MediaType], val dtdValidate: Boolean) {
  private var _baseURI: Option[URI] = None
  private var _dtdValidate: Option[Boolean] = None
  private var _location: Option[Location] = None
  private var _params: Option[Map[QName,XdmValue]] = None
  private var _docprops: Option[Map[QName,XdmValue]] = None

  def this(href: URI) {
    this(href, None, false)
  }

  def this(href: URI, contentType: MediaType) {
    this(href, Some(contentType), false)
  }

  def baseURI: Option[URI] = _baseURI
  def baseURI_=(base: URI): Unit = {
    if (_baseURI.isEmpty) {
      _baseURI = Some(base)
    } else {
      throw new IllegalArgumentException("Only a single assigment to baseURI is allowed")
    }
  }

  def location: Option[Location] = _location
  def location_=(loc: Location): Unit = {
    if (_location.isEmpty) {
      _location = Some(loc)
    } else {
      throw new IllegalArgumentException("Only a single assigment to location is allowed")
    }
  }

  def params: Map[QName,XdmValue] = {
    if (_params.isEmpty) {
      HashMap.empty[QName,XdmValue]
    } else {
      _params.get
    }
  }
  def params_=(map: Map[QName,XdmValue]): Unit = {
    if (_params.isEmpty) {
      _params = Some(map)
    } else {
      throw new IllegalArgumentException("Only a single assigment to params is allowed")
    }
  }

  def docprops: Map[QName,XdmValue] = {
    if (_docprops.isEmpty) {
      HashMap.empty[QName,XdmValue]
    } else {
      _docprops.get
    }
  }
  def docprops_=(map: Map[QName,XdmValue]): Unit = {
    if (_docprops.isEmpty) {
      _docprops = Some(map)
    } else {
      throw new IllegalArgumentException("Only a single assigment to docprops is allowed")
    }
  }
}
