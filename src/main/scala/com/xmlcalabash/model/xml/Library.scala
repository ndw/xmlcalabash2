package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants

class Library(override val config: XMLCalabash,
              override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _psviRequired: Option[Boolean] = None
  private var _xpathVersion: Option[String] = None
  private var _excludeInlinePrefixes = Map.empty[String,String]
  private var _version: Option[String] = None

  override def validate(): Boolean = {
    var valid = true

    _psviRequired = lexicalBoolean(attributes.get(XProcConstants._psvi_required))
    _xpathVersion = attributes.get(XProcConstants._xpath_version)
    _excludeInlinePrefixes = lexicalPrefixes(attributes.get(XProcConstants._exclude_inline_prefixes))
    _version = attributes.get(XProcConstants._version)

    for (key <- List(XProcConstants._psvi_required,
      XProcConstants._xpath_version, XProcConstants._exclude_inline_prefixes,
      XProcConstants._version)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    for (key <- attributes.keySet) {
      throw new ModelException(ExceptionCode.BADCONTAINERATTR, key.getLocalName, location)
    }

    val okChildren = List(classOf[DeclareStep], classOf[Function], classOf[Documentation], classOf[PipeInfo])

    valid = true
    for (child <- children) {
      valid = valid && child.validate()
    }

    valid
  }

  override def makePortsExplicit(): Boolean = true
  override def makePipesExplicit(): Boolean = true
  override def makeBindingsExplicit(): Boolean = true
}
