package com.xmlcalabash.steps.text

import com.xmlcalabash.runtime.{XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable.ListBuffer

class TextLines() extends DefaultXmlStep {
  private var text: XdmNode = _
  private var meta: XProcMetadata = _
  protected val lines: ListBuffer[String] = ListBuffer.empty[String]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode =>
        text = node
        lines ++= text.getStringValue.split('\n')
      case _ => throw new RuntimeException("non node to text?")
    }
    meta = metadata
  }
}
