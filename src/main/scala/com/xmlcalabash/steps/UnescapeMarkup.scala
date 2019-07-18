package com.xmlcalabash.steps

import java.io.ByteArrayInputStream

import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.om.{FingerprintedQName, NamespaceBinding}
import net.sf.saxon.s9api._
import net.sf.saxon.tree.util.NamespaceIterator

import scala.collection.mutable.ListBuffer

class UnescapeMarkup() extends DefaultXmlStep {
  private val _content_type = new QName("content-type")
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var namespace = Option.empty[String]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    metadata = meta
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    if (bindings.contains(XProcConstants._namespace)) {
      namespace = Some(stringBinding(XProcConstants._namespace))
    }

    val root = S9Api.documentElement(source)
    if (root.isEmpty) {
      throw new RuntimeException("Input is not an XML document")
    }

    var result: XdmNode = null
    val contentType = MediaType.parse(stringBinding(_content_type))
    if (contentType.xmlContentType) {
      // Special case XML to deal with the needs-a-wrapper case
      val text = "<wrapper>" + root.get.getStringValue + "</wrapper>"
      val stream = new ByteArrayInputStream(text.getBytes("utf-8"))
      val request = new DocumentRequest(source.getBaseURI, contentType, context.location)
      val resp = config.documentManager.parse(request, stream)
      val wrapper = S9Api.documentElement(resp.value.asInstanceOf[XdmNode])
      val tree = new SaxonTreeBuilder(config)
      tree.startDocument(source.getBaseURI)
      for (child <- S9Api.axis(wrapper.get, Axis.CHILD)) {
        tree.addSubtree(child)
      }
      tree.endDocument()
      result = tree.result
    } else {
      val stream = new ByteArrayInputStream(source.getStringValue.getBytes("utf-8"))
      val request = new DocumentRequest(source.getBaseURI, contentType, context.location)
      val resp = config.documentManager.parse(request, stream)
      result = resp.value.asInstanceOf[XdmNode]
    }

    val tree = new SaxonTreeBuilder(config)
    tree.startDocument(source.getBaseURI)
    tree.addStartElement(root.get)
    tree.addAttributes(root.get)
    tree.startContent()

    if (namespace.isDefined) {
      remapDefaultNamespace(tree, result)
    } else {
      tree.addSubtree(result)
    }

    tree.endDocument()

    consumer.get.receive("result", tree.result, metadata)
  }

  private def remapDefaultNamespace(tree: SaxonTreeBuilder, node: XdmNode): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        for (child <- S9Api.axis(node, Axis.CHILD)) {
          remapDefaultNamespace(tree, child)
        }
      case XdmNodeKind.ELEMENT =>
        val inode = node.getUnderlyingNode
        val curNS = ListBuffer.empty[NamespaceBinding]
        var nsiter = NamespaceIterator.iterateNamespaces(inode)
        while (nsiter.hasNext) {
          curNS += nsiter.next
        }

        var replaced = false
        val newNS = ListBuffer.empty[NamespaceBinding]
        for (ns <- curNS) {
          val pfx = ns.getPrefix
          if (pfx == "") {
            newNS += new NamespaceBinding(pfx, namespace.get)
            replaced = true
          } else {
            newNS += ns
          }
        }
        if (!replaced) {
          newNS += new NamespaceBinding("", namespace.get)
        }

        if (node.getNodeName.getNamespaceURI == "") {
          val newName = new FingerprintedQName("", namespace.get, inode.getLocalPart)
          tree.addStartElement(newName, inode.getSchemaType, newNS.toList)
        } else {
          val newName = new FingerprintedQName(inode.getPrefix, inode.getURI, inode.getLocalPart)
          tree.addStartElement(newName, inode.getSchemaType, curNS.toList)
        }
        tree.addAttributes(node)
        for (child <- S9Api.axis(node, Axis.CHILD)) {
          remapDefaultNamespace(tree, child)
        }
        tree.addEndElement()
      case _ =>
        tree.addSubtree(node)
    }
  }
}
