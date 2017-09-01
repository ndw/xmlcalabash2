package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.exceptions.ModelException
import com.xmlcalabash.model.util.ParserConfiguration
import net.sf.saxon.s9api.QName

class OptionDecl(override val config: ParserConfiguration,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _name: QName = new QName("", "UNINITIALIZED")
  private var _required = false
  private var _select = Option.empty[String]

  def optionName: QName = _name
  def required: Boolean = _required
  def select: Option[String] = _select

  override def validate(): Boolean = {
    val qname = lexicalQName(properties.get(XProcConstants._name))
    if (qname.isEmpty) {
      throw new ModelException("namereqd", "An option name is required", location)
    }

    _name = qname.get
    _required = lexicalBoolean(properties.get(XProcConstants._required)).getOrElse(false)
    _select = properties.get(XProcConstants._select)

    for (key <- List(XProcConstants._name, XProcConstants._required, XProcConstants._select)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (properties.nonEmpty) {
      val key = properties.keySet.head
      throw new ModelException("badopt", s"Unexpected attribute: ${key.getLocalName}", location)
    }

    if (children.nonEmpty) {
      throw new ModelException("badelem", s"Unexpected element: ${children.head}", location)
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get
    val cnode = container.graphNode.get.asInstanceOf[ContainerStart]
    if (cnode.parent.isEmpty) {
      graphNode = Some(graph.addBinding(_name.getClarkName))
    } else {
      throw new ModelException("badopt", "Don't know what to do about opts here", location)
    }
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    //graph.addBindingEdge(graphNode.get.asInstanceOf[Binding], parent)
  }
}
