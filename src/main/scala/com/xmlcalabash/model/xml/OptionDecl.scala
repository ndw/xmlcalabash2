package com.xmlcalabash.model.xml

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ExpressionContext, SaxonExpressionOptions, XProcXPathExpression}
import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.s9api.QName
import net.sf.saxon.sxpath.IndependentContext
import net.sf.saxon.trans.XPathException
import net.sf.saxon.value.SequenceType

class OptionDecl(override val config: XMLCalabash,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _name: QName = new QName("", "UNINITIALIZED")
  private var _required = false
  private var _select = Option.empty[String]
  private var _as = Option.empty[SequenceType]

  def optionName: QName = _name
  def required: Boolean = _required
  def select: Option[String] = _select
  def as: Option[SequenceType] = _as

  override def validate(): Boolean = {
    val qname = lexicalQName(attributes.get(XProcConstants._name))
    if (qname.isEmpty) {
      throw new ModelException(ExceptionCode.NAMEATTRREQ, this.toString, location)
    }

    _name = qname.get
    _required = lexicalBoolean(attributes.get(XProcConstants._required)).getOrElse(false)
    _select = attributes.get(XProcConstants._select)

    val seqType = attributes.get(XProcConstants._as)
    if (seqType.isDefined) {
      try {
        val parser = new XPathParser
        parser.setLanguage(XPathParser.SEQUENCE_TYPE, 31)
        val ic = new IndependentContext(config.processor.getUnderlyingConfiguration)
        for ((prefix, uri) <- inScopeNS) {
          ic.declareNamespace(prefix, uri)
        }
        _as = Some(parser.parseSequenceType(seqType.get, ic))
      } catch {
        case xpe: XPathException =>
          throw XProcException.dynamicError(49, List(seqType.get, xpe.getMessage), location)
        case t: Throwable =>
          throw t
      }
    }

    for (key <- List(XProcConstants._name, XProcConstants._required, XProcConstants._select, XProcConstants._as)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    if (children.nonEmpty) {
      throw XProcException.xsElementNotAllowed(location, children.head.nodeName)
    }

    true
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
    if (cnode.parent.nonEmpty) {
      throw new ModelException(ExceptionCode.INTERNAL, "Don't know what to do about opts here", location)
    }

    val context = new ExpressionContext(_baseURI, inScopeNS, _location)
    val options = new SaxonExpressionOptions(Map("collection" -> false, "optiondecl" -> true))
    val init = new XProcXPathExpression(context, _select.getOrElse("()"), as)
    val node = graph.addOption(_name.getClarkName, init)
    _graphNode = Some(node)
    config.addNode(node.id, this)
  }
  /*
    val container = this.parent.get
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
    val context = new ExpressionContext(_baseURI, inScopeNS, _location)
    val options = new SaxonExpressionOptions(Map("collection" -> _collection))
    _expression = Some(new XProcXPathExpression(context, _select.get, as))
    val node = cnode.addVariable(_name.getClarkName, expression, options)
    _graphNode = Some(node)
    config.addNode(node.id, this)

    for (child <- children) {
      child.makeGraph(graph, parent)
    }

   */

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    if (_select.isDefined) {
      val context = new ExpressionContext(_baseURI, inScopeNS, _location)
      val variableRefs = findVariableRefs(new XProcXPathExpression(context, _select.get))
      for (ref <- variableRefs) {
        val bind = findBinding(ref)
        if (bind.isEmpty) {
          throw new ModelException(ExceptionCode.NOBINDING, ref.toString, location)
        }
        graph.addBindingEdge(bind.get._graphNode.get.asInstanceOf[Binding], _graphNode.get)
      }
    }
  }
}
