package com.xmlcalabash.model.xml

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ExpressionContext, SaxonExpressionOptions, XMLCalabashRuntime, XProcExpression, XProcXPathExpression}
import net.sf.saxon.s9api.{QName, XdmValue}
import net.sf.saxon.value.SequenceType

import scala.collection.mutable

class OptionDecl(override val config: XMLCalabashRuntime,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _name: QName = new QName("", "UNINITIALIZED")
  private var _required = false
  private var _select = Option.empty[String]
  private var _expression = Option.empty[XProcExpression]
  private var _as = Option.empty[SequenceType]
  private var _declaredType = Option.empty[String]
  private var _static = false
  private var _externalValue = Option.empty[XdmValue]
  //private var _staticValueMessage = Option.empty[XdmValueItemMessage]

  def optionName: QName = _name
  def required: Boolean = _required
  def select: Option[String] = _select
  def expression: XProcExpression = _expression.get
  def as: Option[SequenceType] = _as
  def declaredType: String = _declaredType.getOrElse("xs:string")

  def static: Boolean = _static

  def externalValue: Option[XdmValue] = _externalValue
  def externalValue_=(value: XdmValue): Unit = {
    _externalValue = Some(value)
  }
  def externalValue_=(value: Option[XdmValue]): Unit = {
    _externalValue = value
  }

  /*
  def staticValueMessage: Option[XdmValueItemMessage] = {
    if (_static && _staticValueMessage.isDefined) {
      _staticValueMessage
    } else {
      None
    }
  }
  */

  override def validate(): Boolean = {
    var valid = super.validate()

    val qname = lexicalQName(attributes.get(XProcConstants._name))
    if (qname.isEmpty) {
      throw new ModelException(ExceptionCode.NAMEATTRREQ, this.toString, location)
    }
    _name = qname.get

    _required = lexicalBoolean(attributes.get(XProcConstants._required)).getOrElse(false)
    _select = attributes.get(XProcConstants._select)
    _static = lexicalBoolean(attributes.get(XProcConstants._static)).getOrElse(false)

    _declaredType = attributes.get(XProcConstants._as)
    _as = sequenceType(attributes.get(XProcConstants._as))

    if (_static) {
      if (_select.isEmpty) {
        throw XProcException.xsNoSelectOnStaticOption(location)
      }

      val context = new ExpressionContext(staticContext)
      val varExpr = new XProcXPathExpression(context, _select.get, _as)
      val bindingRefs = lexicalVariables(_select.get)

      /*
      val staticVariableMap = mutable.HashMap.empty[String, XdmValueItemMessage]
      for (vref <- bindingRefs) {
        val msg = staticValue(vref)
        if (msg.isDefined) {
          staticVariableMap.put(vref.getClarkName, msg.get)
        } else {
          throw new ModelException(ExceptionCode.NOBINDING, vref.toString, location)
        }
      }
      val eval = config.expressionEvaluator
      if (config.staticOptionValue(optionName).isDefined) {
        logger.debug(s"Using static option value: $optionName = ${config.staticOptionValue(optionName).get}")
        _staticValueMessage = Some(eval.precomputedValue(varExpr, config.staticOptionValue(optionName).get, List(), staticVariableMap.toMap, None))
      } else {
        _staticValueMessage = Some(eval.value(varExpr, List(), staticVariableMap.toMap, None))
      }
     */
    }

    for (key <- List(XProcConstants._name, XProcConstants._required, XProcConstants._as, XProcConstants.cx_as,
      XProcConstants._select, XProcConstants._static)) {
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

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
    if (cnode.parent.nonEmpty) {
      throw new ModelException(ExceptionCode.INTERNAL, "Don't know what to do about opts here", location)
    }

    if (static) {
      val node = graph.addStaticOption(_name.getClarkName, None)
      _graphNode = Some(node)
      config.addNode(node.id, this)
    } else {
      val context = new ExpressionContext(staticContext)
      val options = new SaxonExpressionOptions(Map("collection" -> false, "optiondecl" -> true))
      val init = new XProcXPathExpression(context, _select.getOrElse("()"), as)
      val node = graph.addOption(_name.getClarkName, init, None)
      _graphNode = Some(node)
      config.addNode(node.id, this)
    }
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    if (_select.isDefined && !_static) {
      val context = new ExpressionContext(staticContext)
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
