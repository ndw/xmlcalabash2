package com.xmlcalabash.model.xml.util

import net.sf.saxon.Controller
import net.sf.saxon.expr.instruct.Executable
import net.sf.saxon.s9api._
import net.sf.saxon.event.{NamespaceReducer, PipelineConfiguration, Receiver}
import net.sf.saxon.tree.util.NamespaceIterator
import java.net.URI

import com.xmlcalabash.core.{XProcEngine, XProcException}
import net.sf.saxon.`type`.{BuiltInType, SchemaType, SimpleType}
import net.sf.saxon.om._
import net.sf.saxon.trans.XPathException

class TreeWriter private {
  protected var engine: XProcEngine = _
  protected var processor: Processor = _
  protected var controller: Controller = _
  protected var pool: NamePool = _
  protected var exec: Executable = _
  protected var destination: XdmDestination = _
  protected var receiver: Receiver = _
  protected var seenRoot: Boolean = false
  protected var inDocument: Boolean = false

  def this(xproc: XProcEngine) {
    this()
    engine = xproc
    processor = engine.processor
    controller = new Controller(processor.getUnderlyingConfiguration)
    pool = processor.getUnderlyingConfiguration.getNamePool
  }

  def getResult: XdmNode = {
    destination.getXdmNode
  }

  def startDocument(baseURI: URI) {
    inDocument = true
    seenRoot = false
    try {
      exec = new Executable(controller.getConfiguration)
      destination = new XdmDestination
      receiver = destination.getReceiver(controller.getConfiguration)
      receiver = new NamespaceReducer(receiver)
      val pipe: PipelineConfiguration = controller.makePipelineConfiguration
      receiver.setPipelineConfiguration(pipe)
      if (baseURI != null) {
        receiver.setSystemId(baseURI.toASCIIString)
      }
      else {
        receiver.setSystemId("http://example.com/")
      }
      receiver.open()
      receiver.startDocument(0)
    }
    catch {
      case e: Exception => engine.dynamicError(e)
    }
  }

  def endDocument() {
    try {
      receiver.endDocument()
      receiver.close()
    }
    catch {
      case e: Exception => engine.dynamicError(e)
    }
  }

  def addSubtree(node: XdmNode) {
    if (node.getNodeKind eq XdmNodeKind.DOCUMENT) {
      writeChildren(node)
    }
    else if (node.getNodeKind eq XdmNodeKind.ELEMENT) {
      addStartElement(node)
      val iter: XdmSequenceIterator = node.axisIterator(Axis.ATTRIBUTE)
      while (iter.hasNext) {
        val child: XdmNode = iter.next.asInstanceOf[XdmNode]
        addAttribute(child, child.getStringValue)
      }
      try {
        receiver.startContent()
      }
      catch {
        case xe: XPathException => engine.dynamicError(xe)
      }
      writeChildren(node)
      addEndElement()
    }
    else if (node.getNodeKind eq XdmNodeKind.COMMENT) {
      addComment(node.getStringValue)
    }
    else if (node.getNodeKind eq XdmNodeKind.TEXT) {
      addText(node.getStringValue)
    }
    else if (node.getNodeKind eq XdmNodeKind.PROCESSING_INSTRUCTION) {
      addPI(node.getNodeName.getLocalName, node.getStringValue)
    }
    else {
      engine.dynamicError(new XProcException("Unexpected node type"))
    }
  }

  protected def writeChildren(node: XdmNode) {
    val iter: XdmSequenceIterator = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child: XdmNode = iter.next.asInstanceOf[XdmNode]
      addSubtree(child)
    }
  }

  def addStartElement(node: XdmNode) {
    val inode = node.getUnderlyingNode
    var inscopeNS: Array[NamespaceBinding] = null
    if (seenRoot) {
      inscopeNS = inode.getDeclaredNamespaces(null)
    }
    else {
      var count = 0
      var nsiter  = NamespaceIterator.iterateNamespaces(inode)
      while (nsiter.hasNext) {
        count += 1
        nsiter.next
      }
      inscopeNS = new Array[NamespaceBinding](count)
      nsiter = NamespaceIterator.iterateNamespaces(inode)
      count = 0
      while (nsiter.hasNext) {
        inscopeNS(count) = nsiter.next
        count += 1
      }
      seenRoot = true
    }
    val nodeBaseURI: URI = node.getBaseURI
    receiver.setSystemId(nodeBaseURI.toASCIIString)

    val name = node.getNodeName()
    val elemName = new FingerprintedQName(name.getPrefix, name.getNamespaceURI, name.getLocalName)
    addStartElement(elemName, inode.getSchemaType, inscopeNS)
  }

  def addStartElement(node: XdmNode, newName: QName) {
    val inode = node.getUnderlyingNode
    var inscopeNS: Array[NamespaceBinding] = null
    if (seenRoot) {
      inscopeNS = inode.getDeclaredNamespaces(null)
    }
    else {
      var count = 0
      var nsiter = NamespaceIterator.iterateNamespaces(inode)
      while (nsiter.hasNext) {
        count += 1
        nsiter.next
      }
      inscopeNS = new Array[NamespaceBinding](count)
      nsiter = NamespaceIterator.iterateNamespaces(inode)
      count = 0
      while (nsiter.hasNext) {
        inscopeNS(count) = nsiter.next
        count += 1
      }
      seenRoot = true
    }

    if ("" == newName.getPrefix) {
      var newLen = 0
      for (pos <- 0 to inscopeNS.length) {
        val nscode = inscopeNS(pos)
        if ("" != nscode.getPrefix) {
          newLen += 1
        }
      }
      if (newLen != inscopeNS.length) {
        val newCodes = new Array[NamespaceBinding](newLen)
        var npos = 0
        for (pos <- 0 to inscopeNS.length) {
          val nscode = inscopeNS(pos)
          if ("" != nscode.getPrefix) {
            newCodes(npos) = nscode
            npos += 1
          }
        }
        inscopeNS = newCodes
      }
    }

    val nodeBaseURI = node.getBaseURI
    receiver.setSystemId(nodeBaseURI.toASCIIString)
    val newNameOfNode = new FingerprintedQName(newName.getPrefix, newName.getNamespaceURI, newName.getLocalName)
    addStartElement(newNameOfNode, inode.getSchemaType, inscopeNS)
  }

  def addStartElement(newName: QName) {
    val elemName = new FingerprintedQName(newName.getPrefix, newName.getNamespaceURI, newName.getLocalName)
    val typeCode = BuiltInType.getSchemaType(StandardNames.XS_UNTYPED)
    val inscopeNS: Array[NamespaceBinding] = null
    addStartElement(elemName, typeCode, inscopeNS)
  }

  def addStartElement(elemName: NodeName, typeCode: SchemaType, nscodes: Array[NamespaceBinding]) {
    val loc = new XProcSourceLocator(receiver.getSystemId)
    try {
      receiver.startElement(elemName, typeCode, loc, 0)
      if (nscodes != null) {
        for (ns <- nscodes) {
          receiver.namespace(ns, 0)
        }
      }
    }
    catch {
      case e: XPathException => engine.dynamicError(e)
    }
  }

  def addNamespace(prefix: String, uri: String) {
    val nsbind = new NamespaceBinding(prefix, uri)
    try {
      receiver.namespace(nsbind, 0)
    }
    catch {
      case e: XPathException => engine.dynamicError(e)
    }
  }

  def addAttributes(element: XdmNode) {
    val iter = element.axisIterator(Axis.ATTRIBUTE)
    while (iter.hasNext) {
      val child: XdmNode = iter.next.asInstanceOf[XdmNode]
      addAttribute(child)
    }
  }

  def addAttribute(xdmattr: XdmNode) {
    addAttribute(xdmattr, xdmattr.getStringValue)
  }

  def addAttribute(xdmattr: XdmNode, newValue: String) {
    val loc = new XProcSourceLocator(receiver.getSystemId)
    val inode = xdmattr.getUnderlyingNode

    val name = xdmattr.getNodeName()
    val attrName = new FingerprintedQName(name.getPrefix, name.getNamespaceURI, name.getLocalName)
    val typeCode = inode.getSchemaType.asInstanceOf[SimpleType]
    try {
      receiver.attribute(attrName, typeCode, newValue, loc, 0)
    }
    catch {
      case e: XPathException => engine.dynamicError(e)
    }
  }

  def addAttribute(elemName: NodeName, typeCode: SimpleType, newValue: String) {
    val loc = new XProcSourceLocator(receiver.getSystemId)
    try {
      receiver.attribute(elemName, typeCode, newValue, loc, 0)
    }
    catch {
      case e: XPathException => engine.dynamicError(e)
    }
  }

  def addAttribute(attrName: QName, newValue: String) {
    val loc = new XProcSourceLocator(receiver.getSystemId)
    val elemName = new FingerprintedQName(attrName.getPrefix, attrName.getNamespaceURI, attrName.getLocalName)
    val typeCode = BuiltInType.getSchemaType(StandardNames.XS_UNTYPED_ATOMIC).asInstanceOf[SimpleType]
    try {
      receiver.attribute(elemName, typeCode, newValue, loc, 0)
    }
    catch {
      case e: XPathException => engine.dynamicError(e)
    }
  }

  def startContent() {
    try {
      receiver.startContent()
    }
    catch {
      case xe: XPathException => engine.dynamicError(xe)
    }
  }

  def addEndElement() {
    try {
      receiver.endElement()
    }
    catch {
      case e: XPathException => engine.dynamicError(e)
    }
  }

  def addComment(comment: String) {
    val loc = new XProcSourceLocator(receiver.getSystemId)
    try {
      receiver.comment(comment, loc, 0)
    }
    catch {
      case e: XPathException => engine.dynamicError(e)
    }
  }

  def addText(text: String) {
    val loc = new XProcSourceLocator(receiver.getSystemId)
    try {
      receiver.characters(text, loc, 0)
    }
    catch {
      case e: XPathException => engine.dynamicError(e)
    }
  }

  def addPI(target: String, data: String) {
    val loc = new XProcSourceLocator(receiver.getSystemId)
    try {
      receiver.processingInstruction(target, data, loc, 0)
    }
    catch {
      case e: XPathException => engine.dynamicError(e)
    }
  }
}
