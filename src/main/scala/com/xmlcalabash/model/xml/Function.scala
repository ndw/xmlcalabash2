package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.QName

class Function(override val config: XMLCalabash,
               override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _fName: Option[QName] = None
  private var _fClass: Option[String] = None

  def functionName: QName = _fName.get
  def functionClass: String = _fClass.get

  override def validate(): Boolean = {
    var valid = true

    _fName = lexicalQName(attributes.get(XProcConstants._name))
    if (_fName.isEmpty) {
      throw new RuntimeException("Function name is required")
    }

    _fClass = config.functionImplementation(_fName.get)
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
      valid = valid && child.validate()
    }

    valid
  }
}
