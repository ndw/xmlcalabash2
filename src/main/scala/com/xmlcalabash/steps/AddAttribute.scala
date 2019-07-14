package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{Axis, QName, XdmNode}
import net.sf.saxon.value.QNameValue

import scala.collection.mutable

class AddAttribute() extends DefaultXmlStep with ProcessMatchingNodes {
  private val _attribute_name = new QName("", "attribute-name")
  private val _attribute_value = new QName("", "attribute-value")
  private var pattern: String = _
  private var matcher: ProcessMatch = _
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var attrName: QName = _
  private var attrValue: String = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (port == "source") {
      source = item.asInstanceOf[XdmNode]
      this.metadata = metadata
    } else {
      throw new IllegalArgumentException(s"p:add-attribute received input on port: $port")
    }
  }

  override def run(context: StaticContext): Unit = {
    val qn = bindings(_attribute_name).value.getUnderlyingValue.asInstanceOf[QNameValue]
    attrName = new QName(qn.getPrefix, qn.getNamespaceURI, qn.getLocalName)
    attrValue = bindings(_attribute_value).getStringValue
    pattern = bindings(XProcConstants._match).getStringValue

    if (attrName.getLocalName == "xmlns"
      || attrName.getPrefix == "xmlns"
      || XProcConstants.ns_xmlns == attrName.getNamespaceURI
      || (attrName.getPrefix != "xml" && attrName.getNamespaceURI == XProcConstants.ns_xml)
      || (attrName.getPrefix == "xml" && attrName.getNamespaceURI != XProcConstants.ns_xml)
    ) {
      throw XProcException.xcCannotAddNamespaces(attrName, location)
    }

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    consumer.get.receive("result", matcher.result, metadata)
  }

  override def startDocument(node: XdmNode): Boolean = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def startElement(node: XdmNode): Boolean = {
    // We're going to loop through the attributes several times, so let's grab them.
    // If the element has an attribute named attrName, skip it because we're going to replace it.
    val nsbindings = mutable.HashMap.empty[String, String]
    if (node.getNodeName.getPrefix != "") {
      nsbindings.put(node.getNodeName.getPrefix, node.getNodeName.getNamespaceURI)
    }

    val attrs = mutable.HashMap.empty[QName, String]
    val iter = node.axisIterator(Axis.ATTRIBUTE)
    while (iter.hasNext) {
      val attr = iter.next
      if (attr.getNodeName != attrName) {
        attrs.put(attr.getNodeName, attr.getStringValue)
        if (attr.getNodeName.getPrefix != "") {
          nsbindings.put(attr.getNodeName.getPrefix, attr.getNodeName.getNamespaceURI)
        }
      }
    }

    // Ok, add the element to the output
    matcher.addStartElement(node)

    var instanceAttrName = attrName
    if (attrName.getNamespaceURI != null && attrName.getNamespaceURI != "") {
      var prefix = attrName.getPrefix
      val ns = attrName.getNamespaceURI
      // If the requested prefix is bound to something else, drop it.
      if (nsbindings.contains(prefix) && ns != nsbindings(prefix)) {
        prefix = ""
      }

      // If there isn't a prefix, invent one
      if (prefix == "") {
        val prefix = S9Api.uniquePrefix(nsbindings.keySet.toSet)
        matcher.addNamespace(prefix, attrName.getNamespaceURI)
        instanceAttrName = new QName(prefix, attrName.getNamespaceURI, attrName.getLocalName)
      }
    }

    // Add the "new" attribute in, with its instance-valid QName
    matcher.addAttribute(instanceAttrName, attrValue)

    // Add the other attributes
    for (attr <- attrs.keySet) {
      matcher.addAttribute(attr, attrs(attr))
    }

    true
  }

  override def endElement(node: XdmNode): Unit = {
    matcher.addEndElement()
  }

  override def endDocument(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def attribute(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "attribute", location)
  }

  override def text(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "text", location)
  }

  override def comment(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "comment", location)
  }

  override def pi(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "processing-instruction",
      location)
  }
}
