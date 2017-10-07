package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, SaxonExpressionOptions, XProcXPathExpression}
import net.sf.saxon.s9api.QName

class Variable(override val config: XMLCalabash,
               override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _name: QName = new QName("", "UNINITIALIZED")
  private var _collection: Boolean = false
  private var _select = Option.empty[String]

  def variableName: QName = _name
  def select: Option[String] = _select

  override def validate(): Boolean = {
    val qname = lexicalQName(attributes.get(XProcConstants._name))
    if (qname.isEmpty) {
      throw new ModelException(ExceptionCode.NAMEATTRREQ, this.toString, location)
    }
    _name = qname.get

    _select = attributes.get(XProcConstants._select)
    if (_select.isEmpty) {
      throw new ModelException(ExceptionCode.SELECTATTRREQ, this.toString, location)
    }

    _collection = lexicalBoolean(attributes.get(XProcConstants._collection)).getOrElse(false)

    for (key <- List(XProcConstants._name, XProcConstants._required, XProcConstants._select, XProcConstants._collection)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    if (relevantChildren().nonEmpty) {
      throw new ModelException(ExceptionCode.BADCHILD, children.head.toString, location)
    }

    true
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
    val context = new ExpressionContext(_baseURI, inScopeNS, _location)
    val options = new SaxonExpressionOptions(Map("collection" -> _collection))
    val node = cnode.addVariable(_name.getClarkName, new XProcXPathExpression(context, _select.get), options)
    _graphNode = Some(node)
    config.addNode(node.id, this)
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    val drp = defaultReadablePort
    if (drp.isDefined) {
      val src = drp.get.parent.get
      graph.addEdge(src._graphNode.get, drp.get.port.get, _graphNode.get, "source")
    }
  }
}
