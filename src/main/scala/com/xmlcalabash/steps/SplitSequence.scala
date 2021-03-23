package com.xmlcalabash.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.expr.LastPositionFinder
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.s9api.{QName, XdmNode}
import net.sf.saxon.tree.iter.ManualIterator

import scala.collection.mutable.ListBuffer

class SplitSequence() extends DefaultXmlStep {
  private val _initial_only = new QName("", "initial-only")

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.ZERO_OR_MORE),
    Map("source" -> List("application/xml", "text/html"))
  )
  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("matched" -> PortCardinality.ZERO_OR_MORE, "not-matched" -> PortCardinality.ZERO_OR_MORE),
    Map("matched" -> List("application/xml", "text/html"), "not-matched" -> List("application/xml", "text/html"))
  )

  val sources = ListBuffer.empty[Document]

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode =>
        sources += new Document(node, metadata)
      case _ =>
        throw new RuntimeException("Unexpected node type")
    }
  }

  override def run(staticContext: StaticContext): Unit = {
    var initialOnly = bindings(_initial_only).getUnderlyingValue.effectiveBooleanValue()
    var testExpr = bindings(XProcConstants._test).getUnderlyingValue.getStringValue
    var more = true
    var index = 0

    val fakeLastPositionFinder = new MyLastPositionFinder()

    for (source <- sources) {
      index += 1
      if (more) {
        val compiler = config.processor.newXPathCompiler()
        if (staticContext.baseURI.isDefined) {
          compiler.setBaseURI(staticContext.baseURI.get)
        }
        for ((pfx, uri) <- bindingContexts(XProcConstants._test).nsBindings) {
          compiler.declareNamespace(pfx, uri)
        }
        val exec = compiler.compile(testExpr)
        val expr = exec.getUnderlyingExpression

        val dyncontext = expr.createDynamicContext()
        val context = dyncontext.getXPathContextObject

        var fakeIterator = new ManualIterator(source.source.getUnderlyingNode, index)
        fakeIterator.setLastPositionFinder(fakeLastPositionFinder)
        context.setCurrentIterator(fakeIterator)
        val value = expr.evaluate(dyncontext)

        val matches = value.size() match {
          case 0 => false
          case 1 => value.get(0).effectiveBooleanValue()
          case _ => true
        }

        if (matches) {
          consumer.get.receive("matched", source.source, source.meta)
        } else {
          consumer.get.receive("not-matched", source.source, source.meta)
          more = !initialOnly
        }
      } else {
        consumer.get.receive("not-matched", source.source, source.meta)
      }
    }
  }

  protected class Document(val source: XdmNode, val meta: XProcMetadata) {
    // nop
  }

  protected class MyLastPositionFinder extends LastPositionFinder {
    override def getLength: Int = sources.size
  }
}
