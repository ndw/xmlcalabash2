package com.xmlcalabash.steps.json

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import net.sf.saxon.s9api.{XdmArray, XdmAtomicValue, XdmItem, XdmMap, XdmValue}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer


class Merge extends DefaultXmlStep {
  private val inputs = ListBuffer.empty[XdmMap]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.JSONSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.JSONRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case xdm: XdmMap => inputs += xdm
      case _ => throw new RuntimeException("Only objects may be passed to p:json-merge")
    }
  }

  override def run(staticContext: StaticContext) {
    var merged = new XdmMap()
    for (item <- inputs) {
      for ((key,value) <- item.asImmutableMap().asScala) {
        merged = merged.put(key, value)
      }
    }

    consumer.get.receive("result", merged, XProcMetadata.JSON)
  }

  override def reset(): Unit = {
    super.reset()
    inputs.clear()
  }
}
