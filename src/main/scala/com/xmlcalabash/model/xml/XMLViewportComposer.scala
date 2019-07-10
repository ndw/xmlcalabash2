package com.xmlcalabash.model.xml

import com.jafpl.messages.{Message, Metadata}
import com.jafpl.steps.{ViewportComposer, ViewportItem}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.messages.{XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XProcVtExpression}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmNode, XdmValue}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XMLViewportComposer(config: XMLCalabashConfig, context: StaticContext, patternString: String) extends ViewportComposer {
  private val cx_viewport = new QName("cx", XProcConstants.ns_cx,"viewport")
  private val _index = new QName("index")
  private var matcher: ProcessMatch = _
  private var decomposed: XdmNode = _
  private var metadata: XProcMetadata = _
  private var items = ListBuffer.empty[XMLViewportItem]
  private var itemIndex = 0
  private var pattern = patternString
  private var dynBindings = Map.empty[String, Message]

  override def runtimeBindings(bindings: Map[String, Message]): Unit = {
    dynBindings = bindings
  }

  override def decompose(message: Message): List[ViewportItem] = {
    var source: XdmNode = null

    message match {
      case msg: XdmNodeItemMessage =>
        source = msg.item
        metadata = msg.metadata
      case _ =>
        throw new RuntimeException("viewport expects an xml document")
    }

    val patternAvt = ValueParser.parseAvt(patternString)
    if (patternAvt.isDefined && patternAvt.get.size > 1) {
      val bindings = mutable.HashMap.empty[String,Message] ++ context.statics
      for ((name, message) <- dynBindings) {
        bindings.put(name,message)
      }

      val expr = new XProcVtExpression(context, patternAvt.get)
      var msg = config.expressionEvaluator.singletonValue(expr, List(), bindings.toMap, None)
      // Ok, now we have a string value
      pattern = msg.item.getUnderlyingValue.getStringValue
    }

    matcher = new ProcessMatch(config, new Decomposer(), context)
    matcher.process(source, pattern)
    decomposed = matcher.result
    items.toList
  }

  override def recompose(): Message = {
    matcher = new ProcessMatch(config, new Recomposer(), context)
    matcher.process(decomposed, "*") // FIXME: match only cx:viewport
    val recomposed = matcher.result
    new XdmNodeItemMessage(recomposed, metadata, context)
  }

  private class Decomposer() extends ProcessMatchingNodes {
    private def insertMarker(): Unit = {
      matcher.addStartElement(cx_viewport)
      matcher.addAttribute(_index, itemIndex.toString)
      matcher.startContent()
      itemIndex += 1
    }

    override def startDocument(node: XdmNode): Boolean = {
      items += new XMLViewportItem(node)
      insertMarker()
      matcher.addEndElement()
      false
    }

    override def startElement(node: XdmNode): Boolean = {
      items += new XMLViewportItem(node)
      insertMarker()
      false
    }

    override def endElement(node: XdmNode): Unit = {
      matcher.addEndElement()
    }

    override def endDocument(node: XdmNode): Unit = {
      matcher.endDocument()
    }

    override def attribute(node: XdmNode): Unit = {
      throw new RuntimeException("viewport cannot process attributes")
    }

    override def text(node: XdmNode): Unit = {
      items += new XMLViewportItem(node)
      insertMarker()
      matcher.addEndElement()
    }

    override def comment(node: XdmNode): Unit = {
      items += new XMLViewportItem(node)
      insertMarker()
      matcher.addEndElement()
    }

    override def pi(node: XdmNode): Unit = {
      items += new XMLViewportItem(node)
      insertMarker()
      matcher.addEndElement()
    }
  }

  private class Recomposer() extends ProcessMatchingNodes {
    private def processMarker(marker: XdmNode): Unit = {
      val index = marker.getAttributeValue(_index).toInt
      matcher.addSubtree(items(index).getReplacement)
    }

    override def startDocument(node: XdmNode): Boolean = {
      matcher.startDocument(node.getBaseURI)
      true
    }

    override def startElement(node: XdmNode): Boolean = {
      if (node.getNodeName == cx_viewport) {
        processMarker(node)
        false
      } else {
        matcher.addStartElement(node)
        true
      }
    }

    override def endElement(node: XdmNode): Unit = {
      if (node.getNodeName != cx_viewport) {
        matcher.addEndElement()
      }
    }

    override def endDocument(node: XdmNode): Unit = {
      matcher.endDocument()
    }

    override def attribute(node: XdmNode): Unit = {
      throw new RuntimeException("this will never happen")
    }

    override def text(node: XdmNode): Unit = {
      throw new RuntimeException("this will never happen")
    }

    override def comment(node: XdmNode): Unit = {
      throw new RuntimeException("this will never happen")
    }

    override def pi(node: XdmNode): Unit = {
      throw new RuntimeException("this will never happen")
    }
  }

  private class XMLViewportItem(item: XdmNode) extends ViewportItem {
    private var replacement: XdmNode = _

    def getReplacement: XdmNode = replacement

    override def getItem: Any = item
    override def getMetadata: Metadata = XProcMetadata.XML
    override def putItems(items: List[Any]): Unit = {
      val lb = ListBuffer.empty[XdmNode]
      for (item <- items) {
        item match {
          case node: XdmNode =>
            lb += node
          case _ =>
            throw new RuntimeException("Expected node from viewport")
        }
      }

      if (lb.size == 1) {
        replacement = lb.head
      } else {
        val builder = new SaxonTreeBuilder(config)
        if (lb.isEmpty) {
          builder.startDocument(None)
        } else {
          builder.startDocument(lb.head.getBaseURI)
        }
        for (item <- lb) {
          builder.addSubtree(item)
        }
        builder.endDocument()
        replacement = builder.result
      }
    }
  }
}
