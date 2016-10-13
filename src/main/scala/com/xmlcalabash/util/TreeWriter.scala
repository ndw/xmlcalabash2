package com.xmlcalabash.util

import java.net.URI

import com.xmlcalabash.core.XProcEngine
import net.sf.saxon.Controller
import net.sf.saxon.`type`.{BuiltInType, SchemaType, SimpleType}
import net.sf.saxon.event.{NamespaceReducer, PipelineConfiguration, Receiver}
import net.sf.saxon.expr.instruct.Executable
import net.sf.saxon.expr.parser.Location
import net.sf.saxon.om._
import net.sf.saxon.s9api._
import net.sf.saxon.tree.util.NamespaceIterator

class TreeWriter private {
  protected var engine: XProcEngine = _
  protected var controller: Controller = _
  protected var pool: NamePool = _
  protected var exec: Executable = _
  protected var destination: XdmDestination = _
  protected var receiver: Receiver = _
  protected var seenRoot: Boolean = false
  protected var inDocument: Boolean = false

  def this(engine: XProcEngine) {
    this()
    controller = new Controller(engine.processor.getUnderlyingConfiguration)
    pool = engine.processor.getUnderlyingConfiguration.getNamePool
  }

  def getResult: XdmNode = {
    destination.getXdmNode
  }

  def startDocument(baseURI: URI) {
    inDocument = true
    seenRoot = false

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

  def endDocument() {
    receiver.endDocument()
    receiver.close()
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
      receiver.startContent()
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
      // nevermind
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

    val name = node.getNodeName
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
    val loc = new DummyLocator(receiver.getSystemId)
    receiver.startElement(elemName, typeCode, loc, 0)
    if (nscodes != null) {
      for (ns <- nscodes) {
        receiver.namespace(ns, 0)
      }
    }
  }

  def addNamespace(prefix: String, uri: String) {
    val nsbind = new NamespaceBinding(prefix, uri)
    receiver.namespace(nsbind, 0)
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
    val loc = new DummyLocator(receiver.getSystemId)
    val inode = xdmattr.getUnderlyingNode

    val name = xdmattr.getNodeName
    val attrName = new FingerprintedQName(name.getPrefix, name.getNamespaceURI, name.getLocalName)
    val typeCode = inode.getSchemaType.asInstanceOf[SimpleType]
    receiver.attribute(attrName, typeCode, newValue, loc, 0)
  }

  def addAttribute(elemName: NodeName, typeCode: SimpleType, newValue: String) {
    val loc = new DummyLocator(receiver.getSystemId)
    receiver.attribute(elemName, typeCode, newValue, loc, 0)
  }

  def addAttribute(attrName: QName, newValue: String) {
    val loc = new DummyLocator(receiver.getSystemId)
    val elemName = new FingerprintedQName(attrName.getPrefix, attrName.getNamespaceURI, attrName.getLocalName)
    val typeCode = BuiltInType.getSchemaType(StandardNames.XS_UNTYPED_ATOMIC).asInstanceOf[SimpleType]
    receiver.attribute(elemName, typeCode, newValue, loc, 0)
  }

  def startContent() {
    receiver.startContent()
  }

  def addEndElement() {
    receiver.endElement()
  }

  def addComment(comment: String) {
    val loc = new DummyLocator(receiver.getSystemId)
    receiver.comment(comment, loc, 0)
  }

  def addText(text: String) {
    val loc = new DummyLocator(receiver.getSystemId)
    receiver.characters(text, loc, 0)
  }

  def addPI(target: String, data: String) {
    val loc = new DummyLocator(receiver.getSystemId)
    receiver.processingInstruction(target, data, loc, 0)
  }

  class DummyLocator(val sysId: String, val pubId: String, val line: Int, val col: Int) extends Location {
    def this(sysId: String) {
      this(sysId, null, -1, -1)
    }
    def this(sysId: String, line: Int) {
      this(sysId, null, line, -1)
    }
    def this(sysId: String, line: Int, col: Int) {
      this(sysId, null, line, col)
    }

    override def getLineNumber: Int = line

    override def getColumnNumber: Int = col

    override def saveLocation(): Location = this

    override def getSystemId: String = sysId

    override def getPublicId: String = pubId
  }

}
