package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ExpressionContext, SaxonExpressionOptions, XProcXPathExpression}
import net.sf.saxon.s9api.QName

class OptionDecl(override val config: XMLCalabash,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _name: QName = new QName("", "UNINITIALIZED")
  private var _required = false
  private var _select = Option.empty[String]

  def optionName: QName = _name
  def required: Boolean = _required
  def select: Option[String] = _select

  override def validate(): Boolean = {
    val qname = lexicalQName(attributes.get(XProcConstants._name))
    if (qname.isEmpty) {
      throw new ModelException(ExceptionCode.NAMEATTRREQ, this.toString, location)
    }

    _name = qname.get
    _required = lexicalBoolean(attributes.get(XProcConstants._required)).getOrElse(false)
    _select = attributes.get(XProcConstants._select)

    for (key <- List(XProcConstants._name, XProcConstants._required, XProcConstants._select)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    if (children.nonEmpty) {
      throw new ModelException(ExceptionCode.BADCHILD, children.head.toString, location)
    }

    true
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
    if (cnode.parent.isEmpty) {
      val context = new ExpressionContext(_baseURI, inScopeNS, _location)
      val options = new SaxonExpressionOptions(Map("collection" -> false, "optiondecl" -> true))
      val node = graph.addBinding(_name.getClarkName)
      _graphNode = Some(node)
      config.addNode(node.id, this)
    } else {
      throw new ModelException(ExceptionCode.INTERNAL, "Don't know what to do about opts here", location)
    }
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    // There are no edges to top-level options
  }
}
