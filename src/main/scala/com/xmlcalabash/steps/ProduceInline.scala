package com.xmlcalabash.steps

import java.net.URI

import com.jafpl.exceptions.PipelineException
import com.xmlcalabash.model.util.{AvtParser, SaxonTreeBuilder}
import com.xmlcalabash.model.xml.XProcConstants
import com.xmlcalabash.runtime.{SaxonExpressionEvaluator, XProcAvtExpression, XProcExpression, XProcXPathExpression, XmlMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{Axis, QName, XdmMap, XdmNode, XdmNodeKind}

import scala.collection.mutable

class ProduceInline(private val nodes: List[XdmNode],
                    private val nsBindings: Map[String,String],
                    private val expandText: Boolean,
                    private val excludeInlinePrefixes: Map[String,String],
                    private val docPropsExpr: Option[String],
                    private val encoding: Option[String]) extends DefaultStep {
  private val include_expand_text_attribute = false
  private val docProps = mutable.HashMap.empty[String, String]
  private val excludeURIs = excludeInlinePrefixes.values.toSet

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(): Unit = {
    if (docPropsExpr.isDefined) {
      val expr = new XProcXPathExpression(nsBindings, docPropsExpr.get)
      val result = xpathValue(expr)
      result match {
        case map: XdmMap =>
          // Grovel through a Java Map
          val iter = map.keySet().iterator()
          while (iter.hasNext) {
            val key = iter.next()
            val value = map.get(key)

            // XProc document property map values are strings
            var strvalue = ""
            val viter = value.iterator()
            while (viter.hasNext) {
              val item = viter.next()
              strvalue += item.getStringValue
            }
            docProps.put(key.getStringValue, strvalue)
          }
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
    consumer.get.receive("result", result, new XmlMetadata("application/xml", docProps.toMap))
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
            builder.addNamespace(ns.getNodeName.getLocalName, ns.getStringValue)
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
        val expanded = if (expandText) {
          expandString(node.getStringValue)
        } else {
          node.getStringValue
        }
        if (expanded != "") {
          builder.addText(expanded)
        }
      case _ =>
        builder.addSubtree(node)
    }
  }

  // FIXME: should return a list of XdmNode
  private def expandString(text: String): String = {
    val evaluator = config.get.expressionEvaluator().asInstanceOf[SaxonExpressionEvaluator]
    val list = AvtParser.parse(text)
    val expr = new XProcAvtExpression(Map.empty[String,String], list.get)
    evaluator.value(expr, List.empty[Any], bindings.toMap).toString
  }

  def xpathValue(expr: XProcExpression): Any = {
    val eval = config.get.expressionEvaluator().asInstanceOf[SaxonExpressionEvaluator]
    eval.withContext(this) { eval.value(expr, List.empty[Any], bindings.toMap) }
  }
}
