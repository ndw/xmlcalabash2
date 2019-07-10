package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.`type`.SimpleType
import net.sf.saxon.om.{FingerprintedQName, NameOfNode, NamespaceBinding, NodeInfo, NodeName}
import net.sf.saxon.s9api.{Axis, QName, XdmNode}
import net.sf.saxon.value.QNameValue

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class NamespaceRename() extends DefaultXmlStep with ProcessMatchingNodes {
  private val _from = new QName("from")
  private val _to = new QName("to")
  private val _apply_to = new QName("apply-to")
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var from: String = _
  private var to: String = _
  private var applyTo: String = _
  private var matcher: ProcessMatch = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    metadata = meta
  }

  override def run(context: StaticContext): Unit = {
    from = bindings(_from).getStringValue
    to = bindings(_to).getStringValue
    if (bindings.contains(_apply_to)) {
      applyTo = bindings(_apply_to).getStringValue
    } else {
      applyTo = "all"
    }

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, "*")

    consumer.get.receive("result", matcher.result, metadata)
  }

  override def startDocument(node: XdmNode): Boolean = true

  override def startElement(node: XdmNode): Boolean = {
    var inode = node.getUnderlyingNode
    val inscopeNS = inode.getDeclaredNamespaces(null)
    var newNS = null

    if (applyTo == "attributes") {
      matcher.addStartElement(NameOfNode.makeName(inode), inode.getSchemaType, inscopeNS.toList)
    } else {
      val newNS = ListBuffer.empty[NamespaceBinding]
      for (ns <- inscopeNS) {
        val pfx = ns.getPrefix
        val uri = ns.getURI
        if (from == uri) {
          if ("" == to) {
            // Nevermind; we're throwing namespaces away
          } else {
            newNS += new NamespaceBinding(pfx, to)
          }
        } else {
          newNS += ns
        }
      }

      // Careful, we're messing with the namespace bindings
      // Make sure the nameCode is right...
      var nameCode = NameOfNode.makeName(inode)
      var pfx = nameCode.getPrefix
      val uri = nameCode.getURI
      if (from == uri) {
        if (to == "") {
          pfx = ""
        }
        nameCode = new FingerprintedQName(pfx, to, nameCode.getLocalPart)
      }

      matcher.addStartElement(nameCode, inode.getSchemaType, newNS.toList)
    }

    if (applyTo != "elements") {
      var iter = node.axisIterator(Axis.ATTRIBUTE)
      val curAttr = mutable.HashSet.empty[QName]
      while (iter.hasNext) {
        val attr = iter.next()
        if (attr.getNodeName.getNamespaceURI != from) {
          curAttr += attr.getNodeName
        }
      }

      iter = node.axisIterator(Axis.ATTRIBUTE)
      while (iter.hasNext) {
        val attr = iter.next()
        inode = attr.getUnderlyingNode
        var nameCode = NameOfNode.makeName(inode)

        var pfx = nameCode.getPrefix
        val uri = nameCode.getURI
        if (from == uri) {
          if (pfx == "") {
            pfx = "_1"
          }

          nameCode = new FingerprintedQName(pfx, to, nameCode.getLocalPart)
          val qname = new QName(nameCode.getURI, nameCode.getLocalPart)
          if (curAttr.contains(qname)) {
            throw XProcException.xcAttributeNameCollision(qname, location)
          }
        }

        matcher.addAttribute(nameCode, inode.getSchemaType.asInstanceOf[SimpleType], attr.getStringValue)
      }
    } else {
      matcher.addAttributes(node)
    }

    true
  }

  override def endElement(node: XdmNode): Unit = {
    matcher.addEndElement()
  }

  override def endDocument(node: XdmNode): Unit = {
    matcher.endDocument()
  }

  override def attribute(node: XdmNode): Unit = {
    throw new RuntimeException("Processing attributes can't happen in p:namespace-rename!?")
  }

  override def text(node: XdmNode): Unit = {
    matcher.addText(node.getStringValue)
  }

  override def comment(node: XdmNode): Unit = {
    matcher.addComment(node.getStringValue)
  }

  override def pi(node: XdmNode): Unit = {
    matcher.addPI(node.getNodeName.getLocalName, node.getStringValue)
  }
}
