package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.expr.LastPositionFinder
import net.sf.saxon.om.{Item, NodeInfo}
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode}
import net.sf.saxon.tree.iter.ManualIterator

import scala.collection.mutable.ListBuffer

class WrapSequence extends DefaultXmlStep {
  private val _wrapper = new QName("", "wrapper")
  private val _group_adjacent = new QName("", "group-adjacent")

  private val inputs = ListBuffer.empty[XdmNode]
  private var groupAdjacent = Option.empty[String]
  private var groupAdjacentContext = Option.empty[StaticContext]

  private var wrapper: QName = _
  private var index = 1
  private val fakeLastPositionFinder = new MyLastPositionFinder()

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case xdm: XdmNode => inputs += xdm
      case _ => throw XProcException.xiWrapXML(location)
    }
  }

  override def run(staticContext: StaticContext) {
    wrapper = qnameBinding(_wrapper).get
    groupAdjacent = optionalStringBinding(_group_adjacent)
    if (groupAdjacent.isDefined) {
      groupAdjacentContext = Some(bindingContexts(_group_adjacent))
    }

    if (groupAdjacent.isEmpty) {
      runSimple(staticContext)
    } else {
      runAdjacent(staticContext)
    }
  }

  def runSimple(staticContext: StaticContext): Unit = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(staticContext.baseURI)

    builder.addStartElement(wrapper)
    builder.startContent()
    for (item <- inputs) {
      builder.addSubtree(item)
    }
    builder.addEndElement()
    builder.endDocument()

    consumer.get.receive("result", builder.result, XProcMetadata.XML)
  }

  def runAdjacent(staticContext: StaticContext) {
    var inGroup = false
    var lastValue: Item[_] = null
    var builder: SaxonTreeBuilder = null

    index = 0
    for (item <- inputs) {
      index += 1
      val thisValue = adjacentValue(item)

      if (Option(lastValue).isDefined) {
        if (lastValue.getStringValue == thisValue.getStringValue) {
          builder.addSubtree(item)
        } else {
          if (inGroup) {
            inGroup = false
            builder.addEndElement()
            builder.endDocument()
            consumer.get.receive("result", builder.result, XProcMetadata.XML)
          }
        }
      }

      if (Option(lastValue).isEmpty || lastValue.getStringValue != thisValue.getStringValue) {
        lastValue = thisValue
        inGroup = true
        builder = new SaxonTreeBuilder(config)
        builder.startDocument(staticContext.baseURI)
        builder.addStartElement(wrapper)
        builder.startContent()
        builder.addSubtree(item)
      }
    }

    if (inGroup) {
      inGroup = false
      builder.addEndElement()
      builder.endDocument()
      consumer.get.receive("result", builder.result, XProcMetadata.XML)
    }
  }

  private def adjacentValue(node: XdmNode): Item[_] = {
    val compiler = config.processor.newXPathCompiler()
    compiler.setBaseURI(groupAdjacentContext.get.baseURI.get)
    for ((pfx, uri) <- bindingContexts(_group_adjacent).nsBindings) {
      compiler.declareNamespace(pfx, uri)
    }
    val exec = compiler.compile(groupAdjacent.get)
    val expr = exec.getUnderlyingExpression

    val dyncontext = expr.createDynamicContext()
    val context = dyncontext.getXPathContextObject

    val fakeIterator = new ManualIterator[NodeInfo](node.getUnderlyingNode, index)
    fakeIterator.setLastPositionFinder(fakeLastPositionFinder)
    context.setCurrentIterator(fakeIterator)
    val iter = expr.iterate(dyncontext)
    val value = Option(iter.next())

    if (value.isEmpty) {
      throw new RuntimeException("group-adjacent returned nothing?")
    } else {
      if (Option(iter.next()).isDefined) {
        throw new RuntimeException("group-adjacent returned a sequence")
      }
      value.get.asInstanceOf[Item[_]]
    }
  }

  override def reset(): Unit = {
    super.reset()
    index = 1
    inputs.clear()
  }

  protected class MyLastPositionFinder extends LastPositionFinder {
    override def getLength: Int = inputs.size
  }
}
