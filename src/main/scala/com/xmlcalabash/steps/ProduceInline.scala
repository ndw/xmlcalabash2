package com.xmlcalabash.steps

import java.net.URI

import com.xmlcalabash.model.util.{AvtParser, SaxonTreeBuilder}
import com.xmlcalabash.model.xml.XProcConstants
import com.xmlcalabash.runtime.{XProcAvtExpression, XmlMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}

import scala.collection.mutable

class ProduceInline(private val nodes: List[XdmNode], private val expandText: Boolean) extends DefaultStep {
  private val bindings = mutable.HashMap.empty[String,Any]
  private val include_expand_text_attribute = false

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(variable: String, value: Any): Unit = {
    config.get.trace("debug", s"Document receives binding: $variable: $value", "stepBindings")
    bindings.put(new QName("", variable).getClarkName, value)
  }

  override def run(): Unit = {
    val builder = new SaxonTreeBuilder(config.get)
    builder.startDocument(Some(URI.create("http://example.com/")))
    builder.startContent()
    for (node <- nodes) {
      expandTVT(node, builder, expandText)
    }
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XmlMetadata("application/xml"))
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
        var newExpand = expandText
        var iter = node.axisIterator(Axis.ATTRIBUTE)
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
  // FIXME: what about namespace bindings?
  private def expandString(text: String): String = {
    val evaluator = config.get.expressionEvaluator()
    val list = AvtParser.parse(text)
    val expr = new XProcAvtExpression(Map.empty[String,String], list.get)
    evaluator.value(expr, List.empty[Any], bindings.toMap).toString
  }
}