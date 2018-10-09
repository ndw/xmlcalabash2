package com.xmlcalabash.model.util

import java.net.URI

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.Controller
import net.sf.saxon.`type`.{BuiltInType, SchemaType, SimpleType}
import net.sf.saxon.event.{NamespaceReducer, Receiver}
import net.sf.saxon.expr.instruct.Executable
import net.sf.saxon.om.{FingerprintedQName, NamespaceBinding, NodeName, StandardNames}
import net.sf.saxon.s9api.{Axis, QName, XdmDestination, XdmNode, XdmNodeKind, XdmValue}
import net.sf.saxon.tree.util.NamespaceIterator

import scala.collection.mutable.ListBuffer

class SaxonTreeBuilder(runtime: XMLCalabash) {
  protected val config = runtime.processor.getUnderlyingConfiguration
  protected val pool = config.getNamePool
  protected val controller = new Controller(config)

  protected var exec: Executable = _
  protected var destination: XdmDestination = _
  protected var receiver: Receiver = _
  private var _inDocument = false
  protected var seenRoot = false

  def result: XdmNode = destination.getXdmNode

  def inDocument: Boolean = _inDocument
  protected def inDocument_=(in: Boolean): Unit = {
    _inDocument = in
  }

  def startDocument(baseURI: URI): Unit = {
    startDocument(Some(baseURI))
  }

  def startDocument(baseURI: Option[URI]): Unit = {
    if (baseURI.isDefined) {
      trace(s"startDocument: ${baseURI.get}")
    } else {
      trace("startDocument")
    }

    _inDocument = true
    seenRoot  = false
    try {
      exec = new Executable(controller.getConfiguration)
      destination = new XdmDestination()
      receiver = destination.getReceiver(controller.getConfiguration)
      receiver = new NamespaceReducer(receiver)

      val pipe = controller.makePipelineConfiguration()
      receiver.setPipelineConfiguration(pipe)

      if (baseURI.isDefined) {
        receiver.setSystemId(baseURI.get.toASCIIString)
      } else {
        receiver.setSystemId("http://example.com/")
      }

      receiver.open()
      receiver.startDocument(0)
    } catch {
      case t: Throwable => throw t
    }
  }

  def endDocument(): Unit = {
    trace("endDocument")
    try {
      receiver.setSystemId("http://norman-was-here.com/")
      receiver.endDocument()
      receiver.close()
    } catch {
      case t: Throwable => throw t
    }
  }

  def addSubtree(node: XdmNode): Unit = {
    trace(s"addSubTree: ${node.getNodeKind}", s"$node")
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        writeChildren(node)
      case XdmNodeKind.ELEMENT =>
        addStartElement(node)
        val iter = node.axisIterator(Axis.ATTRIBUTE)
        while (iter.hasNext) {
          val child = iter.next().asInstanceOf[XdmNode]
          addAttribute(child, child.getStringValue)
        }
        try {
          receiver.startContent()
        } catch {
          case t: Throwable => throw t
        }
        writeChildren(node)
        addEndElement()
      case XdmNodeKind.COMMENT =>
        addComment(node.getStringValue)
      case XdmNodeKind.TEXT =>
        addText(node.getStringValue)
      case XdmNodeKind.PROCESSING_INSTRUCTION =>
        addPI(node.getNodeName.getLocalName, node.getStringValue)
      case _ =>
        throw new ModelException(ExceptionCode.BADTREENODE, List(node.getNodeKind.toString, node.getNodeName.toString), node)
    }
  }

  def addValues(values: XdmValue): Unit = {
    addText(S9Api.valuesToString(values))
  }

  protected def writeChildren(node: XdmNode): Unit = {
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      addSubtree(iter.next().asInstanceOf[XdmNode])
    }
  }

  def addStartElement(node: XdmNode): Unit = {
    addStartElement(node, node.getNodeName, node.getBaseURI)
  }

  def addStartElement(node: XdmNode, overrideBaseURI: URI): Unit = {
    addStartElement(node, node.getNodeName, overrideBaseURI)
  }

  def addStartElement(node: XdmNode, newName: QName): Unit = {
    addStartElement(node, newName, node.getBaseURI)
  }

  def addStartElement(newName: QName) {
    val elemName = new FingerprintedQName(newName.getPrefix, newName.getNamespaceURI, newName.getLocalName)
    val typeCode = BuiltInType.getSchemaType(StandardNames.XS_UNTYPED)
    val inscopeNS = List.empty[NamespaceBinding]
    addStartElement(elemName, typeCode, inscopeNS)
  }

  def addStartElement(node: XdmNode, newName: QName, overrideBaseURI: URI): Unit = {
    val inode = node.getUnderlyingNode
    val inscopeNS = ListBuffer.empty[NamespaceBinding]
    if (seenRoot) {
      for (ns <- inode.getDeclaredNamespaces(null)) {
        inscopeNS += ns
      }
    } else {
      val nsiter = NamespaceIterator.iterateNamespaces(inode)
      while (nsiter.hasNext) {
        inscopeNS += nsiter.next()
      }
      seenRoot = true
    }

    // If the newName has no prefix, then make sure we don't pass along some other
    // binding for the default namespace...
    if (newName.getPrefix == "") {
      var defns = Option.empty[NamespaceBinding]
      for (ns <- inscopeNS) {
        if (ns.getPrefix == "") {
          defns = Some(ns)
        }
      }
      if (defns.isDefined) {
        inscopeNS -= defns.get
      }
    }

    // Hack. See comment at top of file
    if (overrideBaseURI.toASCIIString != "") {
      receiver.setSystemId(overrideBaseURI.toASCIIString)
    }

    val newNameOfNode = new FingerprintedQName(newName.getPrefix, newName.getNamespaceURI, newName.getLocalName)
    addStartElement(newNameOfNode, inode.getSchemaType, inscopeNS.toList)
  }

  def addStartElement(elemName: NodeName, typeCode: SchemaType, nscodes: List[NamespaceBinding]): Unit = {
    trace(s"addStartElement: ${elemName}")
    val loc = if (receiver.getSystemId == null) {
      DefaultLocation.voidLocation
    } else {
      new DefaultLocation(receiver.getSystemId)
    }

    try {
      receiver.startElement(elemName, typeCode, loc, 0)
      for (ns <- nscodes) {
        receiver.namespace(ns, 0)
      }
    } catch {
      case t: Throwable => throw t
    }
  }

  def addNamespace(prefix: String, uri: String) {
    trace(s"addNamespace: $prefix=$uri")
    val nsbind = new NamespaceBinding(prefix, uri)
    receiver.namespace(nsbind, 0)
  }

  def addAttributes(element: XdmNode) {
    val iter = element.axisIterator(Axis.ATTRIBUTE)
    while (iter.hasNext) {
      addAttribute(iter.next.asInstanceOf[XdmNode])
    }
  }

  def addAttribute(xdmattr: XdmNode) {
    addAttribute(xdmattr, xdmattr.getStringValue)
  }

  def addAttribute(xdmAttr: XdmNode, newValue: String) {
    trace(s"addAttribute $xdmAttr", s"$newValue")
    val inode = xdmAttr.getUnderlyingNode
    val name = xdmAttr.getNodeName
    val attrName = new FingerprintedQName(name.getPrefix, name.getNamespaceURI, name.getLocalName)
    val typeCode = inode.getSchemaType.asInstanceOf[SimpleType]
    val loc = new DefaultLocation(receiver.getSystemId)
    receiver.attribute(attrName, typeCode, newValue, loc, 0)
  }

  def addAttribute(elemName: NodeName, typeCode: SimpleType, newValue: String) {
    trace(s"addAttribute $elemName", s"$newValue")
    val loc = new DefaultLocation(receiver.getSystemId)
    receiver.attribute(elemName, typeCode, newValue, loc, 0)
  }

  def addAttribute(attrName: QName, newValue: String) {
    trace(s"addAttribute $attrName", s"$newValue")
    val loc = new DefaultLocation(receiver.getSystemId)
    val elemName = new FingerprintedQName(attrName.getPrefix, attrName.getNamespaceURI, attrName.getLocalName)
    val typeCode = BuiltInType.getSchemaType(StandardNames.XS_UNTYPED_ATOMIC).asInstanceOf[SimpleType]
    receiver.attribute(elemName, typeCode, newValue, loc, 0)
  }

  def startContent() {
    trace("startContent")
    receiver.startContent()
  }

  def addEndElement() {
    trace("endElement")
    receiver.endElement()
  }

  def addComment(comment: String) {
    trace("addComment", comment)
    val loc = new DefaultLocation(receiver.getSystemId)
    receiver.comment(comment, loc, 0)
  }

  def addText(text: String) {
    trace("addText", text)
    val loc = new DefaultLocation(receiver.getSystemId)
    receiver.characters(text, loc, 0)
  }

  def addPI(target: String, data: String) {
    trace("addPI", s"$target=$data")
    val loc = new DefaultLocation(receiver.getSystemId)
    receiver.processingInstruction(target, data, loc, 0)
  }

  private def trace(msg: String): Unit = {
    runtime.trace("info", msg, "TreeConstruction")
  }

  private def trace(msg: String, detail: String): Unit = {
    runtime.trace("info", msg, "TreeConstruction")
    runtime.trace("debug", detail, "TreeConstruction")
  }
}
