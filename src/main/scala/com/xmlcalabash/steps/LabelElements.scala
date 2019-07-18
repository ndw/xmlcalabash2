package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime._
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmNode}
import net.sf.saxon.value.QNameValue

class LabelElements() extends DefaultXmlStep with ProcessMatchingNodes {
  private val _attribute = new QName("attribute")
  private val _label = new QName("label")
  private val _replace = new QName("replace")
  private val p_index = new QName("p", XProcConstants.ns_p, "index")

  private var context: StaticContext = _
  private var attribute: QName = _
  private var label: String = _
  private var pattern: String = _
  private var replace = true
  private var matcher: ProcessMatch = _
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var p_count = 1L

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    metadata = meta
  }

  override def run(context: StaticContext): Unit = {
    attribute = qnameBinding(_attribute).get
    label = stringBinding(_label)
    pattern = stringBinding(XProcConstants._match)
    replace = booleanBinding(_replace).getOrElse(false)
    this.context = context

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    consumer.get.receive("result", matcher.result, metadata)
  }

  override def startDocument(node: XdmNode): Boolean = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def startElement(node: XdmNode): Boolean = {
    val exprEval = config.expressionEvaluator.newInstance()
    val expr = new XProcXPathExpression(context, label, None, None, None)
    val msg = new XdmNodeItemMessage(node, metadata, context)
    val countmsg = new XdmValueItemMessage(new XdmAtomicValue(p_count), XProcMetadata.XML, context)
    val result = exprEval.value(expr, List(msg), Map(p_index.getClarkName -> countmsg), None)
    val index = result.item.getUnderlyingValue.getStringValue

    var dup = false
    matcher.addStartElement(node)
    val iter = node.axisIterator(Axis.ATTRIBUTE)
    while (iter.hasNext) {
      val attr = iter.next()
      if (attr.getNodeName == attribute) {
        dup = true
        if (!replace) {
          matcher.addAttribute(attr)
        }
      } else {
        matcher.addAttribute(attr)
      }
    }

    if (!dup || replace) {
      matcher.addAttribute(attribute, index)
      p_count += 1
    }

    matcher.startContent()
    true
  }

  override def endElement(node: XdmNode): Unit = {
    matcher.addEndElement()
  }

  override def endDocument(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def allAttributes(node: XdmNode, matching: List[XdmNode]): Boolean = true

  override def attribute(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "attribute", location)
  }

  override def text(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "text", location)
  }

  override def comment(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "comment", location)
  }

  override def pi(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "processing-instruction", location)
  }
}
