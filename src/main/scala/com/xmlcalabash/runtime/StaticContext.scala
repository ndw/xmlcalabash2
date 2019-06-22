package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.s9api.QName

class StaticContext(init_stepType: QName) {
  protected val _anonymous = new QName("_")
  private var _stepType: QName = init_stepType
  private var _baseURI: Option[URI] = None
  private var _inScopeNS = Map.empty[String,String]
  private var _location: Option[Location] = None

  def this() = {
    this(new QName("_"))
  }

  def stepType: QName = _stepType
  def stepType_=(name: QName): Unit = {
    if (_stepType == _anonymous) {
      _stepType = name
    } else {
      throw new RuntimeException("Attempt to rename step type in static context")
    }
  }

  def baseURI: Option[URI] = _baseURI
  protected[xmlcalabash] def baseURI_=(uri: URI): Unit = {
    _baseURI = Some(uri)
  }

  def inScopeNS: Map[String,String] = _inScopeNS
  protected[xmlcalabash] def inScopeNS_=(bindings: Map[String,String]): Unit = {
    _inScopeNS = bindings
  }

  def location: Option[Location] = _location
  protected[xmlcalabash] def location_=(loc: Location): Unit = {
    _location = Some(loc)
  }

  def copy(context: StaticContext): Unit = {
    _stepType = context._stepType
    _baseURI = context._baseURI
    _inScopeNS = context._inScopeNS
    _location = context._location
  }
}
