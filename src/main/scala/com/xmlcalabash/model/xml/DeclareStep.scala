package com.xmlcalabash.model.xml

import com.jafpl.config.Jafpl
import com.jafpl.graph.Graph
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.xml.containers.Container
import com.xmlcalabash.runtime.XProcExpression
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DeclareStep(override val config: XMLCalabash,
                  override val parent: Option[Artifact]) extends Container(config, parent) {
  private var _type: Option[QName] = None
  private var _psviRequired: Option[Boolean] = None
  private var _xpathVersion: Option[String] = None
  private var _excludeInlinePrefixes = Map.empty[String,String]
  private var _version: Option[String] = None
  private val options = mutable.HashMap.empty[QName, XProcExpression]

  def pipelineGraph(): Graph = {
    val jafpl = Jafpl.newInstance()
    val graph = jafpl.newGraph()
    val pipeline = graph.addPipeline(name)

    for (port <- inputPorts) {
      graph.addInput(pipeline, "source")
    }

    for (port <- outputPorts) {
      graph.addOutput(pipeline, "result")
    }

    graphNode = Some(pipeline)
    graphChildren(graph, pipeline)
    graphEdges(graph, pipeline)
    graph
  }

  override def makeInputBindingsExplicit(): Boolean = {
    true // Input bindings on a pipeline do not have to be bound
  }

  override def validate(): Boolean = {
    _name = attributes.get(XProcConstants._name)
    _type = lexicalQName(attributes.get(XProcConstants._type))
    _psviRequired = lexicalBoolean(attributes.get(XProcConstants._psvi_required))
    _xpathVersion = attributes.get(XProcConstants._xpath_version)
    _excludeInlinePrefixes = lexicalPrefixes(attributes.get(XProcConstants._exclude_inline_prefixes))
    _version = attributes.get(XProcConstants._version)
    for (key <- List(XProcConstants._name, XProcConstants._type, XProcConstants._psvi_required,
      XProcConstants._xpath_version, XProcConstants._exclude_inline_prefixes,
      XProcConstants._version)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    for (key <- attributes.keySet) {
      if (key.getNamespaceURI == "") {
        throw new ModelException(ExceptionCode.BADCONTAINERATTR, key.getLocalName, location)
      } else {
        options.put(key, lexicalAvt(key.toString, attributes(key)))
      }
    }

    val groupOne = List(classOf[Input], classOf[Output], classOf[OptionDecl],
      classOf[Log], classOf[Serialization], classOf[Documentation], classOf[PipeInfo])
    val groupTwo = List(classOf[DeclareStep], classOf[Import], classOf[Documentation], classOf[PipeInfo])
    val groupThree = subpiplineClasses ++ List(classOf[Documentation], classOf[PipeInfo])

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
    while (index < children.length && groupThree.contains(children(index).getClass)) {
      valid = valid && children(index).validate()
      index += 1
    }
    if (index < children.length) {
      throw new ModelException(ExceptionCode.BADCHILD, children(index).toString, location)
    }

    valid = valid && makePortsExplicit()
    valid = valid && makeBindingsExplicit()

    valid
  }

  override def asXML: xml.Elem = {
    if (_excludeInlinePrefixes.nonEmpty) {
      var excludeIPs = ""
      for (prefix <- _excludeInlinePrefixes) {
        if (excludeIPs != "") {
          excludeIPs += " "
        }
        excludeIPs += prefix
      }
      dumpAttr("exclude-inline-prefixes", excludeIPs)
    }

    dumpAttr("version", _version)
    dumpAttr("psvi-required", _psviRequired)
    dumpAttr("xpath-version", _xpathVersion)

    if (_type.isDefined) {
      dumpAttr("type", _type.get.getClarkName)
    }

    dumpAttr("name", _name)
    dumpAttr("id", id.toString)

    val nodes = ListBuffer.empty[xml.Node]
    nodes += xml.Text("\n")
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "declare-step", dump_attr.getOrElse(xml.Null), namespaceScope, false, nodes:_*)
  }

}
