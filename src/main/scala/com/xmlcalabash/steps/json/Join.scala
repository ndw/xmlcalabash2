package com.xmlcalabash.steps.json

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import net.sf.saxon.s9api.{XdmArray, XdmItem, XdmValue}

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._


class Join extends DefaultXmlStep {
  private val inputs = ListBuffer.empty[XdmValue]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.JSONSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.JSONRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case xdm: XdmItem => inputs += xdm
      case _ => throw XProcException.xiWrapXML(location)
    }
  }

  override def run(staticContext: StaticContext) {
    val items = ListBuffer.empty[XdmValue]
    for (item <- inputs) {
      item match {
        case array: XdmArray =>
          for (elem <- array.asList().asScala) {
            items += elem
          }
        case _ =>
          items += item
      }
    }

    consumer.get.receive("result", new XdmArray(items.toArray), XProcMetadata.JSON)
  }

  override def reset(): Unit = {
    super.reset()
    inputs.clear()
  }
}
