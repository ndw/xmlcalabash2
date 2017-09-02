package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.ParserConfiguration
import com.xmlcalabash.runtime.XProcXPathExpression
import net.sf.saxon.s9api.QName

class Variable(override val config: ParserConfiguration,
               override val parent: Option[Artifact]) extends Artifact(config, parent) {

  private var _name: QName = new QName("", "UNINITIALIZED")
  private var _select = Option.empty[String]

  def variableName: QName = _name
  def select: Option[String] = _select

  override def validate(): Boolean = {
    val qname = lexicalQName(properties.get(XProcConstants._name))
    if (qname.isEmpty) {
      throw new ModelException(ExceptionCode.NAMEATTRREQ, this.toString, location)
    }
    _name = qname.get

    _select = properties.get(XProcConstants._select)
    if (_select.isEmpty) {
      throw new ModelException(ExceptionCode.SELECTATTRREQ, this.toString, location)
    }

    for (key <- List(XProcConstants._name, XProcConstants._required, XProcConstants._select)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (properties.nonEmpty) {
      val key = properties.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    if (relevantChildren().nonEmpty) {
      throw new ModelException(ExceptionCode.BADCHILD, children.head.toString, location)
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get
    val cnode = container.graphNode.get.asInstanceOf[ContainerStart]
    graphNode = Some(cnode.addVariable(_name.getClarkName, new XProcXPathExpression(inScopeNS, _select.get)))
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    val drp = defaultReadablePort()
    if (drp.isDefined) {
      val src = drp.get.parent.get
      graph.addEdge(src.graphNode.get, drp.get.port.get, graphNode.get, "source")
    }
  }
}
