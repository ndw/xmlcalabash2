package com.xmlcalabash.model.util

import java.net.URI

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.{DefaultLocation, S9Api}
import net.sf.saxon.{Configuration, Controller}
import net.sf.saxon.`type`.{BuiltInType, SchemaType, SimpleType}
import net.sf.saxon.event.{NamespaceReducer, Receiver}
import net.sf.saxon.expr.instruct.Executable
import net.sf.saxon.om.{FingerprintedQName, NamePool, NamespaceBinding, NodeName, StandardNames}
import net.sf.saxon.s9api.{Axis, QName, XdmDestination, XdmNode, XdmNodeKind, XdmValue}
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.tree.util.NamespaceIterator

import scala.collection.mutable.ListBuffer

class SaxonTreeBuilder(runtime: XMLCalabashConfig) {
  protected val config: Configuration = runtime.processor.getUnderlyingConfiguration
  protected val pool: NamePool = config.getNamePool
  protected val controller: Controller = new Controller(config)

  protected var exec: Executable = _
  protected var destination: XdmDestination = _
  protected var receiver: Receiver = _
  private var _inDocument = false
  protected var seenRoot = false

  def this(runtime: XMLCalabashRuntime) = {
    this(runtime.config)
  }

  def result: XdmNode = destination.getXdmNode

  def inDocument: Boolean = _inDocument
  protected def inDocument_=(in: Boolean): Unit = {
    _inDocument = in
  }

  def startDocument(baseURI: URI): Unit = {
    startDocument(Option(baseURI))
  }

  def startDocument(baseURI: Option[URI]): Unit = {
    _inDocument = true
    seenRoot  = false
    try {
      exec = new Executable(controller.getConfiguration)
      destination = new XdmDestination()
      val pipe = controller.makePipelineConfiguration()
      receiver = destination.getReceiver(pipe, new SerializationProperties())
      receiver = new NamespaceReducer(receiver)

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
    try {
      receiver.setSystemId("http://norman-was-here.com/")
      receiver.endDocument()
      receiver.close()
    } catch {
      case t: Throwable => throw t
    }
  }

  def addSubtree(node: XdmNode): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        writeChildren(node)
      case XdmNodeKind.ELEMENT =>
        addStartElement(node)
        val iter = node.axisIterator(Axis.ATTRIBUTE)
        while (iter.hasNext) {
          val child = iter.next()
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
      addSubtree(iter.next())
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

  def addStartElement(newName: QName): Unit = {
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

  def addNamespace(prefix: String, uri: String): Unit = {
    val nsbind = new NamespaceBinding(prefix, uri)
    receiver.namespace(nsbind, 0)
  }

  def addAttributes(element: XdmNode): Unit = {
    val iter = element.axisIterator(Axis.ATTRIBUTE)
    while (iter.hasNext) {
      addAttribute(iter.next)
    }
  }

  def addAttribute(xdmattr: XdmNode): Unit = {
    addAttribute(xdmattr, xdmattr.getStringValue)
  }

  def addAttribute(xdmAttr: XdmNode, newValue: String): Unit = {
    val inode = xdmAttr.getUnderlyingNode
    val name = xdmAttr.getNodeName
    val attrName = new FingerprintedQName(name.getPrefix, name.getNamespaceURI, name.getLocalName)
    val typeCode = inode.getSchemaType.asInstanceOf[SimpleType]
    val loc = new DefaultLocation(receiver.getSystemId)
    receiver.attribute(attrName, typeCode, newValue, loc, 0)
  }

  def addAttribute(elemName: NodeName, typeCode: SimpleType, newValue: String): Unit = {
    val loc = new DefaultLocation(receiver.getSystemId)
    receiver.attribute(elemName, typeCode, newValue, loc, 0)
  }

  def addAttribute(attrName: QName, newValue: String): Unit = {
    val loc = new DefaultLocation(receiver.getSystemId)
    val elemName = new FingerprintedQName(attrName.getPrefix, attrName.getNamespaceURI, attrName.getLocalName)
    val typeCode = BuiltInType.getSchemaType(StandardNames.XS_UNTYPED_ATOMIC).asInstanceOf[SimpleType]
    receiver.attribute(elemName, typeCode, newValue, loc, 0)
  }

  def startContent(): Unit = {
    receiver.startContent()
  }

  def addEndElement(): Unit = {
    receiver.endElement()
  }

  def addComment(comment: String): Unit = {
    val loc = new DefaultLocation(receiver.getSystemId)
    receiver.comment(comment, loc, 0)
  }

  def addText(text: String): Unit = {
    val loc = new DefaultLocation(receiver.getSystemId)
    receiver.characters(text, loc, 0)
  }

  def addPI(target: String, data: String): Unit = {
    val loc = new DefaultLocation(receiver.getSystemId)
    receiver.processingInstruction(target, data, loc, 0)
  }
}
