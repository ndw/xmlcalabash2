package com.xmlcalabash.steps.text

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmNode}

import scala.collection.mutable.ListBuffer

class Join() extends DefaultXmlStep {
  private val _separator = new QName("", "separator")
  private val _prefix = new QName("", "prefix")
  private val _suffix = new QName("", "suffix")
  private val docs = ListBuffer.empty[XdmNode]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode => docs += node
      case _ => throw new RuntimeException("non node to text?")
    }
  }

  override def run(context: StaticContext): Unit = {
    val separator = if (bindings.contains(_separator)) {
      bindings(_separator).getStringValue
    } else {
      ""
    }

    var result = if (bindings.contains(_prefix)) {
      bindings(_prefix).getStringValue
    } else {
      ""
    }

    var first = true
    for (node <- docs) {
      if (!first) {
        result += separator
      }
      result += node.getStringValue
      first = false
    }

    if (bindings.contains(_suffix)) {
      result += bindings(_suffix).getStringValue
    }

    var contentType = MediaType.TEXT
    if (bindings.contains(XProcConstants._override_content_type)) {
      contentType = MediaType.parse(bindings(XProcConstants._override_content_type).getStringValue)
    }

    consumer.get.receive("result", new XdmAtomicValue(result), new XProcMetadata(contentType))
  }
}
