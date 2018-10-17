package com.xmlcalabash.model.xml

import java.net.URI

import com.jafpl.graph.Location
import com.jafpl.messages.Message
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.ValueParser
import com.xmlcalabash.runtime.{ExpressionContext, XMLCalabashRuntime, XProcMetadata, XProcVtExpression, XProcXPathExpression}
import net.sf.saxon.s9api.{QName, XdmNode}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Injectable(val config: XMLCalabashRuntime,
                 val id: String,
                 val itype: QName,
                 val stepXPath: XProcXPathExpression,
                 val conditionXPath: XProcXPathExpression,
                 val baseURI: Option[URI],
                 val location: Location) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var _messageXPath = Option.empty[XProcVtExpression]
  private var _messageNodes = Option.empty[List[XdmNode]]
  private val _nodes = ListBuffer.empty[XdmNode]
  private var _port = Option.empty[String]
  private var _matched = false

  def messageXPath: Option[XProcVtExpression] = _messageXPath
  def messageXPath_=(expr: XProcVtExpression): Unit = {
    if (messageXPath.isDefined || messageNodes.isDefined) {
      throw XProcException.xiInjectMessageNodes(Some(location))
    }
    _messageXPath = Some(expr)
  }

  def messageNodes: Option[List[XdmNode]] = _messageNodes
  def messageNodes_=(nodes: List[XdmNode]): Unit = {
    if (messageXPath.isDefined || messageNodes.isDefined) {
      throw XProcException.xiInjectMessageNodes(Some(location))
    }
    _messageNodes = Some(nodes)
  }

  def port: Option[String] = _port
  def port_=(port: String): Unit = {
    if (_port.isDefined) {
      throw XProcException.xiInjectRedefPort(Some(location))
    }
    _port = Some(port)
  }

  def nodes: List[XdmNode] = _nodes.toList
  def matched: Boolean = _matched

  def findSteps(root: XdmNode): Unit = {
    val exprEval = config.expressionEvaluator
    val findContext = new XPathItemMessage(root, XProcMetadata.XML, ExpressionContext.NONE)
    val found = exprEval.value(stepXPath, List(findContext), Map.empty[String,Message], None)

    if (found.item.size() == 0) {
      logger.warn(s"Injector XPath expression matches nothing: ${stepXPath.expr}")
    } else {
      val iter = found.item.iterator()
      while (iter.hasNext) {
        val item = iter.next()
        item match {
          case node: XdmNode =>
            _nodes += node
          case _ =>
            logger.warn(s"Injector XPath expression matches non-node item: ${stepXPath.expr}")
        }
      }
    }
  }

  def findVariableRefs: Set[QName] = {
    val refs = mutable.HashSet.empty[QName]

    if (messageXPath.isDefined) {
      refs ++= ValueParser.findVariableRefs(config, messageXPath.get, Some(location))
    } else {
      for (node <- messageNodes.get) {
        refs ++= ValueParser.findVariableRefs(config, node, expandText=true, Some(location))
      }
    }

    refs ++= ValueParser.findVariableRefs(config, conditionXPath, Some(location))

    refs.toSet
  }

  def matches(node: XdmNode): Boolean = {
    if (baseURI.isEmpty || (node.getBaseURI == baseURI.get)) {
      if (_nodes.contains(node)) {
        _matched = true
        true
      } else {
        false
      }
    } else {
      logger.warn(s"Skipping ${stepXPath.expr}, base URI doesn't match: ${baseURI.get}")
      false
    }
  }
}
