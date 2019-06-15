package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{Axis, QName, XdmNode}

import scala.collection.mutable

class AddAttribute() extends DefaultXmlStep with ProcessMatchingNodes {
  private val _attribute_name = new QName("", "attribute-name")
  private val _attribute_prefix = new QName("", "attribute-prefix")
  private val _attribute_namespace = new QName("", "attribute-namespace")
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
    val attrNameStr = bindings(_attribute_name).getStringValue
    val apfx = if (bindings.contains(_attribute_prefix)) {
      Some(bindings(_attribute_prefix).getStringValue)
    } else {
      None
    }
    val ans = if (bindings.contains(_attribute_namespace)) {
      Some(bindings(_attribute_namespace).getStringValue)
    } else {
      None
    }

    if (apfx.isDefined && ans.isEmpty) {
      throw XProcException.xdConflictingNamespaceDeclarations("Prefix specified without a namespace", location)
    }

    attrName = if (attrNameStr.contains(":")) {
      if (ans.isDefined) {
        throw XProcException.xdConflictingNamespaceDeclarations("Namespace specified but name contains a colon", location)
      }
      val pos = attrNameStr.indexOf(":")
      val pfx = attrNameStr.substring(0, pos)
      val local = attrNameStr.substring(pos+1)
      val nsbindings = bindings(_attribute_name).context.nsBindings
      val ns = if (nsbindings.contains(pfx)) {
        nsbindings(pfx)
      } else {
        throw new IllegalArgumentException(s"There is no binding for the prefix: $pfx")
      }

      new QName(pfx, ns, local)
    } else {
      new QName(apfx.getOrElse(""), ans.getOrElse(""), attrNameStr)
    }

    attrValue = bindings(_attribute_value).getStringValue
    pattern = bindings(XProcConstants._match).getStringValue

    if (attrName.getLocalName == "xmlns"
      || attrName.getPrefix == "xmlns"
      || XProcConstants.ns_xmlns == attrName.getNamespaceURI
      || (attrName.getPrefix != "xml" && attrName.getNamespaceURI == XProcConstants.ns_xml)
      || (attrName.getPrefix == "xml" && attrName.getNamespaceURI != XProcConstants.ns_xml)
    ) {
      throw new IllegalArgumentException("Bad attribute name")
    }

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    consumer.get.receive("result", matcher.result, metadata)
  }

  override def startDocument(node: XdmNode): Boolean = {
    throw XProcException.xcNotAnElement(pattern, "document", location)
  }

  override def startElement(node: XdmNode): Boolean = {
    // We're going to loop through the attributes several times, so let's grab them.
    // If the element has an attribute named attrName, skip it because we're going to replace it.
    val attrs = mutable.HashMap.empty[QName, String]
    val iter = node.axisIterator(Axis.ATTRIBUTE)
    while (iter.hasNext) {
      val attr = iter.next.asInstanceOf[XdmNode]
      if (attr.getNodeName != attrName) {
        attrs.put(attrName, attr.getStringValue)
      }
    }

    var instanceAttrName = attrName
    if (attrName.getNamespaceURI != null && attrName.getNamespaceURI != "") {
      var prefix = attrName.getPrefix
      val ns = attrName.getNamespaceURI
      // If the requested prefix is bound to something else, drop it.
      for (attr <- attrs.keySet) {
        if (prefix == attr.getPrefix && attr.getNamespaceURI != ns) {
          prefix = ""
        }
      }

      // If there isn't a prefix, invent one
      if (prefix == "") {
        var acount = 0
        var aprefix = "_0"
        var done = false
        while (!done) {
          acount += 1
          aprefix = s"_$acount"
          done = true

          for (attr <- attrs.keySet) {
            if (aprefix == attr.getPrefix) {
              done = false
            }
          }
        }
        instanceAttrName = new QName(aprefix, attrName.getNamespaceURI, attrName.getLocalName)
      }
    }

    // Ok, add the element to the output
    matcher.addStartElement(node)

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
    throw XProcException.xcNotAnElement(pattern, "document", location)
  }

  override def attribute(node: XdmNode): Unit = {
    throw XProcException.xcNotAnElement(pattern, "attribute", location)
  }

  override def text(node: XdmNode): Unit = {
    throw XProcException.xcNotAnElement(pattern, "text", location)
  }

  override def comment(node: XdmNode): Unit = {
    throw XProcException.xcNotAnElement(pattern, "comment", location)
  }

  override def pi(node: XdmNode): Unit = {
    throw XProcException.xcNotAnElement(pattern, "processing-instruction",
      location)
  }
}
