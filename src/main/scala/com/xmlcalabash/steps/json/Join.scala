package com.xmlcalabash.steps.json

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import net.sf.saxon.s9api.{QName, XdmArray, XdmItem, XdmValue}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class Join extends DefaultXmlStep {
  private val _flatten_to_depth = new QName("", "flatten-to-depth")
  private val inputs = ListBuffer.empty[XdmValue]
  private val items = ListBuffer.empty[XdmValue]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.JSONSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.JSONRESULT

  private var flattenDepth = 0

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case xdm: XdmItem => inputs += xdm
      case _ => throw XProcException.xiWrapXML(location)
    }
  }

  override def run(staticContext: StaticContext) {
    val depth = bindings(_flatten_to_depth).getUnderlyingValue.getStringValue
    if (depth == "unbounded") {
      flattenDepth = Int.MaxValue
    } else {
      try {
        flattenDepth = depth.toInt
      } catch {
        case _: Exception =>
          throw XProcException.xdValueDoesNotSatisfyType(depth, location)
      }
      if (flattenDepth < 0) {
        throw XProcException.xdValueDoesNotSatisfyType(depth, location)
      }
    }

    items.clear()
    for (item <- inputs) {
      processItem(item, 0)
    }

    consumer.get.receive("result", new XdmArray(items.toArray), XProcMetadata.JSON)
  }

  private def processItem(item: XdmValue, depth: Int): Unit = {
    item match {
      case array: XdmArray =>
        if (depth < flattenDepth) {
          for (elem <- array.asList().asScala) {
            processItem(elem, depth+1)
          }
        } else {
          items += array
        }
      case _ =>
        items += item
    }
  }

  override def reset(): Unit = {
    super.reset()
    inputs.clear()
  }
}
