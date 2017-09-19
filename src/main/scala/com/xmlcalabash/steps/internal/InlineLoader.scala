package com.xmlcalabash.steps.internal

import java.net.URI

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.{ItemMessage, Message}
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, UniqueId, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{DynamicContext, ExpressionContext, SaxonExpressionEvaluator, XProcAvtExpression, XProcExpression, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmItem, XdmMap, XdmNode, XdmNodeKind}

import scala.collection.mutable

class InlineLoader(private val nodes: List[XdmNode],
                   private val context: ExpressionContext,
                   private val expandText: Boolean,
                   private val excludeInlinePrefixes: Map[String,String],
                   private val docPropsExpr: Option[String],
                   private val encoding: Option[String]) extends DefaultStep {
  private val include_expand_text_attribute = false
  private var docProps = Map.empty[QName, XdmItem]
  private val excludeURIs = mutable.HashSet.empty[String]

  excludeURIs += XProcConstants.ns_p
  for (uri <- excludeInlinePrefixes.values) {
    excludeURIs += uri
  }

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(): Unit = {
    if (docPropsExpr.isDefined) {
      val expr = new XProcXPathExpression(context, docPropsExpr.get)
      val result = xpathValue(expr)
      docProps = result match {
        case map: XdmMap =>
          ValueParser.parseDocumentProperties(map, location)
        case _ =>
          throw new PipelineException("notmap", "The document-properties attribute must be a map", None)
      }
    }

    val builder = new SaxonTreeBuilder(config.get)
    builder.startDocument(Some(URI.create("http://example.com/")))
    builder.startContent()
    for (node <- nodes) {
      expandTVT(node, builder, expandText)
    }
    builder.endDocument()
    val result = builder.result
    consumer.get.receive("result", new ItemMessage(result, new XProcMetadata("application/xml", docProps)))
  }

  private def expandTVT(node: XdmNode, builder: SaxonTreeBuilder, expandText: Boolean): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        val iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next().asInstanceOf[XdmNode]
          expandTVT(child, builder, expandText)
        }
      case XdmNodeKind.ELEMENT =>
        builder.addStartElement(node.getNodeName)
        var iter = node.axisIterator(Axis.NAMESPACE)
        while (iter.hasNext) {
          val ns = iter.next().asInstanceOf[XdmNode]
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
          val attr = iter.next().asInstanceOf[XdmNode]
          if (attr.getNodeName == XProcConstants.p_expand_text) {
            newExpand = attr.getStringValue == "true"
          }
          if (include_expand_text_attribute || (attr.getNodeName != XProcConstants.p_expand_text)) {
            if (expandText) {
              builder.addAttribute(attr.getNodeName, expandString(attr.getStringValue))
            } else {
              builder.addAttribute(attr)
            }
          }
        }
        iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next().asInstanceOf[XdmNode]
          expandTVT(child, builder, newExpand)
        }
        builder.addEndElement()
      case XdmNodeKind.TEXT =>
        var str = node.getStringValue
        if (expandText && str.contains("{")) {
          str = expandString(str)
        }
        if (str != "") {
          builder.addText(str)
        }
      case _ =>
        builder.addSubtree(node)
    }
  }

  // FIXME: should return a list of XdmNode
  private def expandString(text: String): String = {
    val evaluator = config.get.expressionEvaluator.asInstanceOf[SaxonExpressionEvaluator]
    val expr = new XProcAvtExpression(context, text)
    evaluator.value(expr, List.empty[Message], bindings.toMap).item.toString
  }

  def xpathValue(expr: XProcExpression): XdmItem = {
    val eval = config.get.expressionEvaluator.asInstanceOf[SaxonExpressionEvaluator]
    val dynContext = new DynamicContext()
    val msg = eval.withContext(dynContext) { eval.value(expr, List.empty[Message], bindings.toMap) }
    msg.asInstanceOf[XPathItemMessage].item
  }
}
