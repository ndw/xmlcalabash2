package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{S9Api, TypeUtils}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap, NamespaceMap}
import net.sf.saxon.s9api.{Axis, QName, XdmNode}

import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsScala

class SetAttributes() extends DefaultXmlStep with ProcessMatchingNodes {
  private val _attributes = new QName("attributes")

  private var pattern: String = _
  private var matcher: ProcessMatch = _
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var attributes = mutable.HashMap.empty[QName,String]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (port == "source") {
      source = item.asInstanceOf[XdmNode]
      this.metadata = metadata
    } else {
      throw new IllegalArgumentException(s"p:set-attributes received input on port: $port")
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    pattern = stringBinding(XProcConstants._match)

    val attr = mapBinding(_attributes)
    for ((name,value) <- attr.asMap().asScala) {
      val attrName = name.getQNameValue
      if (attrName.getLocalName == "xmlns"
        || attrName.getPrefix == "xmlns"
        || XProcConstants.ns_xmlns == attrName.getNamespaceURI
        || (attrName.getPrefix != "xml" && attrName.getNamespaceURI == XProcConstants.ns_xml)
        || (attrName.getPrefix == "xml" && attrName.getNamespaceURI != XProcConstants.ns_xml)
      ) {
        throw XProcException.xcCannotAddNamespaces(attrName, location)
      }
      attributes.put(attrName, value.getUnderlyingValue.getStringValue)
    }

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    consumer.get.receive("result", matcher.result, metadata)
  }

  override def startDocument(node: XdmNode): Boolean = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def startElement(node: XdmNode, nodeattr: AttributeMap): Boolean = {
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
      if (!attributes.contains(attr.getNodeName)) {
        attrs.put(attr.getNodeName, attr.getStringValue)
        if (attr.getNodeName.getPrefix != "") {
          nsbindings.put(attr.getNodeName.getPrefix, attr.getNodeName.getNamespaceURI)
        }
      }
    }

    var nsmap = NamespaceMap.emptyMap()
    var amap: AttributeMap = EmptyAttributeMap.getInstance()

    for ((attrName,attrValue) <- attributes) {
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
          nsmap = nsmap.put(prefix, attrName.getNamespaceURI)
          instanceAttrName = new QName(prefix, attrName.getNamespaceURI, attrName.getLocalName)
        }
      }
      // Add the "new" attribute in, with its instance-valid QName
      amap = amap.put(TypeUtils.attributeInfo(instanceAttrName, attrValue))
    }

    // Add the other attributes
    for (attr <- attrs.keySet) {
      amap = amap.put(TypeUtils.attributeInfo(attr, attrs(attr)))
    }

    matcher.addStartElement(node.getNodeName, amap, nsmap)
    true
  }

  override def endElement(node: XdmNode): Unit = {
    matcher.addEndElement()
  }

  override def endDocument(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def attributes(node: XdmNode, matching: AttributeMap, nonMatching: AttributeMap): Option[AttributeMap] = {
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
