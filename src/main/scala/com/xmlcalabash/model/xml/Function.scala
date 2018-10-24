package com.xmlcalabash.model.xml

import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import net.sf.saxon.s9api.QName

class Function(override val config: XMLCalabashRuntime,
               override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _fName: Option[QName] = None
  private var _fClass: Option[String] = None

  def functionName: QName = _fName.get
  def functionClass: String = _fClass.get

  override def validate(): Boolean = {
    var valid = super.validate()

    _fName = lexicalQName(attributes.get(XProcConstants._name))
    if (_fName.isEmpty) {
      throw new RuntimeException("Function name is required")
    }

    _fClass = config.config.functionImplementation(_fName.get)
    if (_fClass.isEmpty) {
      throw new RuntimeException("Function implementation is unknown")
    }

    for (key <- List(XProcConstants._name)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    for (key <- attributes.keySet) {
      throw new ModelException(ExceptionCode.BADCONTAINERATTR, key.getLocalName, location)
    }

    val okChildren = List(classOf[Documentation], classOf[PipeInfo])
    valid = true
    for (child <- children) {
      if (okChildren.contains(child.getClass)) {
        valid = valid && child.validate()
      } else {
        throw XProcException.xsElementNotAllowed(location, child.nodeName)
      }
    }

    valid
  }
}
