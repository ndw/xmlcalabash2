package com.xmlcalabash.util

import com.jafpl.graph.Location
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcVtExpression}
import net.sf.saxon.s9api.{Axis, XdmNode, XdmNodeKind}

import scala.collection.mutable

class TvtExpander(config: XMLCalabashConfig,
                  contextItem: Option[XProcItemMessage],
                  exprContext: StaticContext,
                  msgBindings: Map[String, XProcItemMessage],
                  location: Option[Location]) {
  private val excludeURIs = mutable.HashSet.empty[String]
  private var expandText = false

  def expand(node: XdmNode, initallyExpand: Boolean, exclude: Set[String]): XdmNode = {
    expandText = initallyExpand
    excludeURIs.clear()
    excludeURIs ++= exclude

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(node.getBaseURI)
    builder.startContent()
    // FIXME: trim whitespace
    expandTVT(node, builder, expandText)
    builder.endDocument()
    builder.result
  }

  private def expandTVT(node: XdmNode, builder: SaxonTreeBuilder, expandText: Boolean): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        val iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next()
          expandTVT(child, builder, expandText)
        }
      case XdmNodeKind.ELEMENT =>
        builder.addStartElement(node.getNodeName)
        var iter = node.axisIterator(Axis.NAMESPACE)
        while (iter.hasNext) {
          val ns = iter.next()
          if (!excludeURIs.contains(ns.getStringValue)) {
            val prefix = if (Option(ns.getNodeName).isDefined) {
              ns.getNodeName.getLocalName
            } else {
              ""
            }
            builder.addNamespace(prefix, ns.getStringValue)
          }
        }
        var newExpand = expandText
        iter = node.axisIterator(Axis.ATTRIBUTE)
        while (iter.hasNext) {
          var discardAttribute = false
          val attr = iter.next()
          if (attr.getNodeName == XProcConstants.p_inline_expand_text) {
            if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
              throw XProcException.xsInlineExpandTextNotAllowed(location)
            }
            discardAttribute = true
            newExpand = attr.getStringValue == "true"
          }
          if (attr.getNodeName == XProcConstants._inline_expand_text) {
            if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
              discardAttribute = true
              newExpand = attr.getStringValue == "true"
            }
          }
          if (!discardAttribute) {
            if (expandText) {
              builder.addAttribute(attr.getNodeName, expandString(attr.getStringValue))
            } else {
              builder.addAttribute(attr)
            }
          }
        }
        iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next()
          expandTVT(child, builder, newExpand)
        }
        builder.addEndElement()
      case XdmNodeKind.TEXT =>
        var str = node.getStringValue
        if (expandText && str.contains("{")) {
          expandNodes(str, builder)
        } else {
          builder.addText(str.replace("}}", "}"))
        }
      case _ =>
        builder.addSubtree(node)
    }
  }

  private def expandString(text: String): String = {
    val evaluator = config.expressionEvaluator
    val expr = new XProcVtExpression(exprContext, text)
    var s = ""
    var string = ""
    val iter = evaluator.value(expr, contextItem.toList, msgBindings, None).item.iterator()
    while (iter.hasNext) {
      val next = iter.next()
      string = string + s + next.getStringValue
      s = " "
    }
    string
  }

  private def expandNodes(text: String, builder: SaxonTreeBuilder): Unit = {
    val evaluator = config.expressionEvaluator
    val expr = new XProcVtExpression(exprContext, text)

    val iter = evaluator.value(expr, contextItem.toList, msgBindings, None).item.iterator()
    while (iter.hasNext) {
      val next = iter.next()
      next match {
        case node: XdmNode => builder.addSubtree(node)
        case _ => builder.addText(next.getStringValue)
      }
    }
  }

}
