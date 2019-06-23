package com.xmlcalabash.model.xml

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.jafpl.messages.Message
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ExpressionContext, SaxonExpressionOptions, XMLCalabashRuntime, XProcExpression, XProcXPathExpression}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmValue}
import net.sf.saxon.value.SequenceType

import scala.collection.mutable.ListBuffer

class OptionDecl(override val config: XMLCalabashRuntime,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _name: QName = new QName("", "UNINITIALIZED")
  private var _required = false
  private var _select = Option.empty[String]
  private var _expression = Option.empty[XProcExpression]
  private var _as = Option.empty[SequenceType]
  private var _declaredType = Option.empty[String]
  private var _allowedValues = Option.empty[List[XdmAtomicValue]]
  private var _static = false
  private var _externalValue = Option.empty[XdmValue]
  private var _staticValueMessage = Option.empty[XdmValueItemMessage]

  def optionName: QName = _name
  def required: Boolean = _required
  def select: Option[String] = _select
  def expression: XProcExpression = _expression.get
  def as: Option[SequenceType] = _as
  def declaredType: String = _declaredType.getOrElse("xs:string")
  def allowedValues: Option[List[XdmAtomicValue]] = _allowedValues

  def static: Boolean = _static

  def externalValue: Option[XdmValue] = _externalValue
  def externalValue_=(value: XdmValue): Unit = {
    _externalValue = Some(value)
  }
  def externalValue_=(value: Option[XdmValue]): Unit = {
    _externalValue = value
  }

  def staticValueMessage: Option[XdmValueItemMessage] = {
    if (_static && _staticValueMessage.isDefined) {
      _staticValueMessage
    } else {
      None
    }
  }
  protected[model] def staticValueMessage_=(msg: XdmValueItemMessage): Unit = {
    _staticValueMessage = Some(msg)
  }

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

    if (attributes.contains(XProcConstants._values)) {
      val seq = attributes(XProcConstants._values)
      val evaluator = config.expressionEvaluator
      val expr = new XProcXPathExpression(ExpressionContext.NONE, seq)
      val exlist = evaluator.value(expr, List(), Map.empty[String,Message], None)
      val list = ListBuffer.empty[XdmAtomicValue]
      val iter = exlist.item.iterator()
      while (iter.hasNext) {
        val item = iter.next()
        list += item.asInstanceOf[XdmAtomicValue]
      }
      _allowedValues = Some(list.toList)
    }

    if (_static) {
      if (_select.isEmpty) {
        throw XProcException.xsNoSelectOnStaticOption(location)
      }

      val context = new ExpressionContext(staticContext)
      val varExpr = new XProcXPathExpression(context, _select.get, _as)
      val bindingRefs = lexicalVariables(_select.get)
    }

    for (key <- List(XProcConstants._name, XProcConstants._required, XProcConstants._as, XProcConstants.cx_as,
      XProcConstants._select, XProcConstants._static, XProcConstants._values)) {
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

  /*
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
      if (_allowedValues.isDefined) {
        println("HERE")
      }
      val init = new XProcXPathExpression(context, _select.getOrElse("()"), as, _allowedValues)
      val node = graph.addOption(_name.getClarkName, init, None)
      _graphNode = Some(node)
      config.addNode(node.id, this)
    }
  }
  */

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
