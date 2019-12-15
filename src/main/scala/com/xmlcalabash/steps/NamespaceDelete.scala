package com.xmlcalabash.steps

import java.net.URI

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime._
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmValue}

import scala.collection.mutable

class NamespaceDelete() extends DefaultXmlStep with ProcessMatchingNodes {
  private val pattern = "*"

  private var matcher: ProcessMatch = _
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var namespaces: mutable.HashSet[String] = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    metadata = meta
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    namespaces = mutable.HashSet.empty[String]
    val prefixes = bindings(XProcConstants._prefixes).getUnderlyingValue.getStringValue.split("\\s+")
    for (prefix <- prefixes) {
      val uri = context.nsBindings.get(prefix)
      if (uri.isDefined) {
        namespaces.add(uri.get)
      } else {
        throw XProcException.xcPrefixNotInScope(prefix, location)
      }
    }

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    consumer.get.receive("result", matcher.result, metadata)
  }

  override def startDocument(node: XdmNode): Boolean = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def startElement(node: XdmNode): Boolean = {
    val newName = if (namespaces.contains(node.getNodeName.getNamespaceURI)) {
      new QName("", node.getNodeName.getLocalName)
    } else {
      node.getNodeName
    }

    val unchanged = mutable.HashSet.empty[QName]
    for (attr <- S9Api.axis(node, Axis.ATTRIBUTE)) {
      if (!namespaces.contains(attr.getNodeName.getNamespaceURI)) {
        unchanged.add(attr.getNodeName)
      }
    }

    for (attr <- S9Api.axis(node, Axis.ATTRIBUTE)) {
      if (namespaces.contains(attr.getNodeName.getNamespaceURI)) {
        val newAttrName = new QName("", attr.getNodeName.getLocalName)
        if (unchanged.contains(newAttrName)) {
          throw XProcException.xcNamespaceDeleteCollision(attr.getNodeName.getNamespaceURI, location)
        }
        unchanged.add(newAttrName)
      }
    }

    // Ok, there are no collisions, do the deletes
    matcher.addStartElement(newName)

    for (attr <- S9Api.axis(node, Axis.ATTRIBUTE)) {
      if (namespaces.contains(attr.getNodeName.getNamespaceURI)) {
        val newAttrName = new QName("", attr.getNodeName.getLocalName)
        // FIXME: this is losing the attribute value's type
        matcher.addAttribute(newAttrName, attr.getStringValue)
      } else {
        matcher.addAttribute(attr)
      }
    }
    matcher.startContent()
    true
  }

  override def allAttributes(node: XdmNode, matching: List[XdmNode]): Boolean = true

  override def endElement(node: XdmNode): Unit = {
    matcher.addEndElement()
  }

  override def endDocument(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def attribute(node: XdmNode): Unit = {
    throw new RuntimeException("attribute called in rename")
  }

  override def text(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "text", location)
  }

  override def comment(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "comment", location)
  }

  override def pi(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "processing-instruction", location)
  }
}
