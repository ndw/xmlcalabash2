package com.xmlcalabash.steps

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.XProcMetadata
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{Axis, QName, XdmArray, XdmAtomicValue, XdmMap, XdmNode, XdmNodeKind, XdmValue}

import scala.collection.mutable

class QTProcessor extends DefaultXmlStep {
  def isTrue(value: Option[Any]): Boolean = {
    if (value.isEmpty) {
      return false
    }

    value.get match {
      case string: String => List("1", "true", "yes").contains(string)
      case atomic: XdmAtomicValue => List("1", "true", "yes").contains(atomic.getStringValue)
      case _ => false
    }
  }

  protected def consume(item: XdmValue, port: String, sprop: Map[QName,XdmValue]): Unit = {
    if (item.size() > 1) {
      val iter = item.iterator()
      while (iter.hasNext) {
        consume(iter.next, port, sprop)
      }
      return
    }

    var outputItem = item
    var ctype = Option.empty[MediaType]

    var serialization = new XdmMap()
    for ((key, value) <- sprop) {
      serialization = serialization.put(new XdmAtomicValue(key), value)
    }

    val dprop = mutable.HashMap.empty[QName, XdmValue]
    dprop.put(XProcConstants._serialization, serialization)

    if (sprop.contains(XProcConstants._method)) {
      sprop(XProcConstants._method).toString match {
        case "html" => ctype = Some(MediaType.HTML)
        case "xhtml" => ctype = Some(MediaType.XHTML)
        case "text" => ctype = Some(MediaType.TEXT)
        case _ => ()
      }
    }

    item match {
      case node: XdmNode =>
        node.getNodeKind match {
          case XdmNodeKind.DOCUMENT =>
            var textOnly = true
            for (child <- S9Api.axis(node, Axis.CHILD)) {
              textOnly = textOnly && child.getNodeKind == XdmNodeKind.TEXT
            }
            if (ctype.isEmpty) {
              ctype = if (textOnly) {
                Some(MediaType.TEXT)
              } else {
                Some(MediaType.XML)
              }
            }
          case XdmNodeKind.TEXT =>
            if (ctype.isEmpty) {
              ctype = Some(MediaType.TEXT)
            }
          case _ =>
            if (ctype.isEmpty) {
              ctype = Some(MediaType.XML)
            }
        }

        if (node.getNodeKind != XdmNodeKind.DOCUMENT) {
          val builder = new SaxonTreeBuilder(config)
          builder.startDocument(node.getBaseURI)
          builder.addSubtree(node)
          builder.endDocument()
          outputItem = builder.result
        }

      case _: XdmAtomicValue =>
        ctype = Some(MediaType.JSON)

      case _: XdmArray =>
        ctype = Some(MediaType.JSON)

      case _: XdmMap =>
        ctype = Some(MediaType.JSON)

      case _ =>
        throw new RuntimeException("Unexpected item type produced by XSLT: " + item)
    }

    val mtype = new XProcMetadata(ctype, dprop.toMap)
    consumer.get.receive(port, outputItem, mtype)
  }
}
