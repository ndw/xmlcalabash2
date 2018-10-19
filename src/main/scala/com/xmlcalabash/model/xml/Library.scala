package com.xmlcalabash.model.xml

import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.containers.DeclarationContainer
import com.xmlcalabash.runtime.XMLCalabashRuntime

import scala.collection.mutable.ListBuffer

class Library(override val config: XMLCalabashRuntime,
              override val parent: Option[Artifact]) extends DeclarationContainer(config, parent, XProcConstants.p_library) {
  private var _psviRequired: Option[Boolean] = None
  private var _xpathVersion: Option[String] = None
  private var _excludeInlinePrefixes = Map.empty[String,String]
  private var _version: Option[String] = None

  override def validate(): Boolean = {
    var valid = super.validate()

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

    for (child <- children) {
      if (okChildren.contains(child.getClass)) {
        valid = valid && child.validate()
      } else {
        throw XProcException.xsElementNotAllowed(location, child.nodeName)
      }
    }

    valid
  }

  override def lastChildStep: Option[PipelineStep] = {
    throw new RuntimeException("Cannot call lastChildStep on p:library")
  }

  override def makeInputPortsExplicit(): Boolean = true
  override def makeOutputPortsExplicit(): Boolean = true
  override def makePortsExplicit(): Boolean = true
  override def makePipesExplicit(): Boolean = true
  override def makeBindingsExplicit(): Boolean = true

  override def asXML: xml.Elem = {
    dumpAttr("name", _name)
    val nodes = ListBuffer.empty[xml.Node]
    nodes += xml.Text("\n")
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "library", dump_attr.getOrElse(xml.Null), namespaceScope, false, nodes:_*)
  }
}
