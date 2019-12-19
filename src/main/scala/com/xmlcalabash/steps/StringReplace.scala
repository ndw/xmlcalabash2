package com.xmlcalabash.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmNode, XdmValue}
import net.sf.saxon.tree.iter.ManualIterator

class StringReplace() extends DefaultXmlStep with ProcessMatchingNodes {
  private val _replace = new QName("", "replace")
  private var source: XdmNode = _
  private var source_metadata: XProcMetadata = _
  private var pattern: String = _
  private var matcher: ProcessMatch = _
  private var replace: String = _
  private var replContext: StaticContext = _

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source"->PortCardinality.EXACTLY_ONE),
    Map("source"->List("application/xml", "text/html")))

  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result"->PortCardinality.EXACTLY_ONE),
    Map("result"->List("application/xml", "text/html", "text/plain")))

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    source_metadata = metadata
  }

  override def run(context: StaticContext): Unit = {
    pattern = stringBinding(XProcConstants._match)
    replace = stringBinding(_replace)
    replContext = bindingContexts(_replace)

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    val result = matcher.result
    consumer.get.receive("result", result, checkMetadata(result, source_metadata))
    }

  private def computeReplacement(context: XdmNode): XdmValue = {
    val compiler = config.processor.newXPathCompiler()
    if (replContext.baseURI.isDefined) {
      compiler.setBaseURI(replContext.baseURI.get)
    }
    for ((pfx, uri) <- replContext.nsBindings) {
      compiler.declareNamespace(pfx, uri)
    }
    val expr = compiler.compile(replace)
    val selector = expr.load()
    selector.setContextItem(context)
    selector.evaluate()
  }

  def replaceNode(context: XdmNode): Unit = {
    val value = computeReplacement(context)
    for (pos <- 0 until value.size()) {
      val item = value.itemAt(pos)
      item match {
        case node: XdmNode =>
          matcher.addSubtree(node)
        case _ =>
          matcher.addText(item.getStringValue)
      }
    }
  }

  def replaceAttribute(context: XdmNode): Unit = {
    val value = computeReplacement(context)
    var text = ""
    for (pos <- 0 until value.size()) {
      val item = value.itemAt(pos)
      text = text + item.getStringValue
    }
    matcher.addAttribute(context.getNodeName, text)
  }

  override def startDocument(node: XdmNode): Boolean = {
    replaceNode(node)
    false
  }

  override def startElement(node: XdmNode): Boolean = {
    replaceNode(node)
    false
  }

  override def endElement(node: XdmNode): Unit = {
    // nop, replaced
  }

  override def endDocument(node: XdmNode): Unit = {
    // nop, replaced
  }

  override def allAttributes(node: XdmNode, matching: List[XdmNode]): Boolean = true

  override def attribute(node: XdmNode): Unit = {
    replaceAttribute(node)
  }

  override def text(node: XdmNode): Unit = {
    replaceNode(node)
  }

  override def comment(node: XdmNode): Unit = {
    replaceNode(node)
  }

  override def pi(node: XdmNode): Unit = {
    replaceNode(node)
  }
}
