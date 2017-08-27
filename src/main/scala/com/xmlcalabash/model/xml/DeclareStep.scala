package com.xmlcalabash.model.xml

import com.jafpl.graph.Graph
import net.sf.saxon.s9api.QName

class DeclareStep(override val parent: Option[Artifact]) extends Step(parent) {
  private var _name: Option[String] = None
  private var _type: Option[QName] = None
  private var _psviRequired: Option[Boolean] = None
  private var _xpathVersion: Option[String] = None
  private var _excludeInlinePrefixes: Set[String] = Set()
  private var _version: Option[String] = None
  private var valid = false

  def pipelineGraph(): Graph = {
    val graph = new Graph()
    val pipeline = graph.addPipeline(_name)
    graph
  }

  override def validate(): Boolean = {
    _name = properties.get(XProcConstants._name)
    _type = lexicalQName(properties.get(XProcConstants._type))
    _psviRequired = lexicalBoolean(properties.get(XProcConstants._psvi_required))
    _xpathVersion = properties.get(XProcConstants._xpath_version)
    _excludeInlinePrefixes = lexicalPrefixes(properties.get(XProcConstants._exclude_inline_prefixes))
    _version = properties.get(XProcConstants._version)
    for (key <- List(XProcConstants._name, XProcConstants._type, XProcConstants._psvi_required,
      XProcConstants._xpath_version, XProcConstants._exclude_inline_prefixes,
      XProcConstants._version)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    for (key <- properties.keySet) {
      if (key.getNamespaceURI == "") {
        throw new XmlPipelineException("badopt", s"Unexpected attribute: ${key.getLocalName}")
      }
    }

    val groupOne = List(classOf[Input], classOf[Output], classOf[OptionDecl],
      classOf[Log], classOf[Serialization])
    val groupTwo = List(classOf[DeclareStep], classOf[Import])

    valid = true
    var index = 0
    while (index < children.length && groupOne.contains(children(index).getClass)) {
      valid = valid && children(index).validate()
      index += 1
    }
    while (index < children.length && groupTwo.contains(children(index).getClass)) {
      valid = valid && children(index).validate()
      index += 1
    }
    while (index < children.length && subpiplineClasses.contains(children(index).getClass)) {
      valid = valid && children(index).validate()
      index += 1
    }
    if (index < children.length) {
      throw new XmlPipelineException("badelem", s"Unexpected element: ${children(index)}")
    }

    valid = valid && makeBindingsExplicit()

    valid
  }

}
