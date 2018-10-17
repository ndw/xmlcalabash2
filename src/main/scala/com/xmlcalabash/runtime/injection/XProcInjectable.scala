package com.xmlcalabash.runtime.injection

import java.net.URI

import com.jafpl.graph.Location
import com.jafpl.messages.{ItemMessage, Message}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.model.xml.Injectable
import com.xmlcalabash.runtime.{ExpressionContext, NodeLocation, SaxonExpressionOptions, XMLCalabashRuntime, XProcVtExpression, XProcXPathExpression}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class XProcInjectable(injectable: Injectable) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected val bindings = mutable.HashMap.empty[String,Message]
  protected val _nodes: ListBuffer[XdmNode] = ListBuffer.empty[XdmNode]
  protected var _matched = false
  protected var config: XMLCalabashRuntime = _
  protected var _stepXPath: XProcXPathExpression = _
  protected var _messageXPath: Option[XProcVtExpression] = _
  protected var _messageNodes: Option[List[XdmNode]] = _
  protected var _conditionXPath: XProcXPathExpression = _
  protected var _baseURI: Option[URI] = _
  protected var _location: Location = _
  protected var _name = Option.empty[String]
  protected var _type = Option.empty[QName]
  protected var _id: String = _

  config = injectable.config
  _stepXPath = injectable.stepXPath
  _messageXPath = injectable.messageXPath
  _messageNodes = injectable.messageNodes
  _conditionXPath = injectable.conditionXPath
  _baseURI = injectable.baseURI
  _location = injectable.location
  _id = injectable.id

  def stepXPath: XProcXPathExpression = _stepXPath
  def messageXPath: Option[XProcVtExpression] = _messageXPath
  def messageNodes: Option[List[XdmNode]] = _messageNodes
  def conditionXPath: XProcXPathExpression = _conditionXPath
  def baseURI: Option[URI] = _baseURI
  def location: Location = _location

  def id: String = _id

  def name: String = _name.get
  def name_=(name: String): Unit = {
    _name = Some(name)
  }
  def stepType: QName = _type.get
  def stepType_=(stepType: QName): Unit = {
    _type = Some(stepType)
  }

  def nodes: List[XdmNode] = _nodes.toList

  protected def expandTVT(node: XdmNode, builder: SaxonTreeBuilder, contextNode: List[ItemMessage], context: ExpressionContext, opts: Option[SaxonExpressionOptions]): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        val iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next().asInstanceOf[XdmNode]
          expandTVT(child, builder, contextNode, context, opts)
        }
      case XdmNodeKind.ELEMENT =>
        val newContext = new ExpressionContext(node.getBaseURI, S9Api.inScopeNamespaces(node), new NodeLocation(node))

        builder.addStartElement(node.getNodeName)
        var iter = node.axisIterator(Axis.NAMESPACE)
        while (iter.hasNext) {
          val ns = iter.next().asInstanceOf[XdmNode]
          val prefix = if (Option(ns.getNodeName).isDefined) {
            ns.getNodeName.getLocalName
          } else {
            ""
          }
          builder.addNamespace(prefix, ns.getStringValue)
        }
        iter = node.axisIterator(Axis.ATTRIBUTE)
        while (iter.hasNext) {
          val attr = iter.next().asInstanceOf[XdmNode]
          builder.addAttribute(attr.getNodeName, expandString(attr.getStringValue, contextNode, newContext, opts))
        }
        iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next().asInstanceOf[XdmNode]
          expandTVT(child, builder, contextNode, newContext, opts)
        }
        builder.addEndElement()
      case XdmNodeKind.TEXT =>
        var str = node.getStringValue
        if (str != "") {
          if (str.contains("{")) {
            val iter = expandNodes(str, contextNode, context, opts).item.iterator()
            while (iter.hasNext) {
              val item = iter.next()
              item match {
                case node: XdmNode =>
                  builder.addSubtree(node)
                case _ =>
                  builder.addText(item.getStringValue)
              }
            }
          } else {
            builder.addText(str)
          }
        }
      case _ =>
        builder.addSubtree(node)
    }
  }

  private def expandString(text: String, contextNode: List[ItemMessage], context: ExpressionContext, opts: Option[SaxonExpressionOptions]): String = {
    var s = ""
    val iter = expandNodes(text, contextNode, context, opts).item.iterator()
    while (iter.hasNext) {
      s += iter.next.getStringValue
    }
    s
  }

  private def expandNodes(text: String, contextNode: List[ItemMessage], context: ExpressionContext, opts: Option[SaxonExpressionOptions]): XPathItemMessage = {
    val evaluator = config.expressionEvaluator
    val expr = new XProcVtExpression(context, text)
    evaluator.value(expr, contextNode, bindings.toMap, opts)
  }
}
