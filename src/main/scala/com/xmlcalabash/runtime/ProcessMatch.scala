package com.xmlcalabash.runtime

import java.net.URI
import java.util

import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import net.sf.saxon.om.NamespaceResolver
import net.sf.saxon.s9api.{Axis, XdmDestination, XdmNode, XdmNodeKind}
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.sxpath.{XPathEvaluator, XPathExpression}
import net.sf.saxon.trans.XPathException

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer

class ProcessMatch(runtime: XMLCalabashRuntime, processor: ProcessMatchingNodes, location: Option[Location]) extends SaxonTreeBuilder(runtime) {
  private val SAW_ELEMENT = 1
  private val SAW_WHITESPACE = 2
  private val SAW_TEXT = 4
  private val SAW_PI = 8
  private val SAW_COMMENT = 16

  var matcher: XPathExpression = _
  var nodeCount: Integer = _
  private var saw = 0

  def process(doc: XdmNode, pattern: String): Unit = {
    val xeval = new XPathEvaluator(config)
    val resolver = new MatchingNamespaceResolver(nsBindings(doc))

    xeval.getStaticContext.setNamespaceResolver(resolver)

    try {
      matcher = xeval.createPattern(pattern)
    } catch {
      case ex: XPathException =>  throw XProcException.xdBadValue(pattern, XProcConstants.pxs_XSLTMatchPattern.getLocalName, ex.getMessage, location)
      case t: Exception => throw t
    }

    destination = new XdmDestination()
    val pipe = controller.makePipelineConfiguration()
    receiver = destination.getReceiver(pipe, new SerializationProperties())

    receiver.setPipelineConfiguration(pipe)
    receiver.setSystemId(doc.getBaseURI.toASCIIString)
    receiver.open()

    // If we start a match at an element, fake a document wrapper
    if (doc.getNodeKind != XdmNodeKind.DOCUMENT) {
      startDocument(doc.getBaseURI)
    }

    traverse(doc)

    if (doc.getNodeKind != XdmNodeKind.DOCUMENT) {
      endDocument()
    }

    receiver.close()
  }

  // We've already done a bunch of setup, don't do it again
  override def startDocument(baseURI: URI): Unit = {
    inDocument = true
    seenRoot = false
    receiver.startDocument(0)
  }

  def count(doc: XdmNode, pattern: String, deep: Boolean): Integer = {
    nodeCount = 0

    val xeval = new XPathEvaluator(config)
    val resolver = new MatchingNamespaceResolver(nsBindings(doc))
    xeval.getStaticContext.setNamespaceResolver(resolver)
    matcher = xeval.createPattern(pattern)
    traverse(doc, deep)

    nodeCount
  }

  def getResult: XdmNode = destination.getXdmNode

  def matches(node: XdmNode): Boolean = {
    try {
      val context = matcher.createDynamicContext(node.getUnderlyingNode)
      matcher.effectiveBooleanValue(context)
    } catch {
      case sae: XPathException => false
      case t: Exception => throw t
    }
  }

  private def traverse(node: XdmNode): Unit = {
    val nmatch = matches(node)
    var processChildren = false

    if (!nmatch) {
      node.getNodeKind match {
        case XdmNodeKind.ELEMENT => saw |= SAW_ELEMENT
        case XdmNodeKind.TEXT =>
          if (node.getStringValue.trim == "") {
            saw |= SAW_WHITESPACE
          } else {
            saw |= SAW_TEXT
          }
        case XdmNodeKind.COMMENT => saw |= SAW_COMMENT
        case XdmNodeKind.PROCESSING_INSTRUCTION => saw |= SAW_PI
        case _ => Unit
      }
    }

    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        if (nmatch) {
          processChildren = processor.startDocument(node)
          saw = 0
        } else {
          startDocument(node.getBaseURI)
        }

        if (!nmatch || processChildren) {
          traverseChildren(node)
        }

        if (nmatch) {
          processor.endDocument(node)
        } else {
          endDocument()
        }

      case XdmNodeKind.ELEMENT =>
        if (nmatch) {
          processChildren = processor.startElement(node)
          saw = 0
        } else {
          addStartElement(node)
        }

        if (!nmatch) {
          // Walk through the attributes twice, processing all the *NON* matches first.
          // That way if a matching node renames an attribute, it can replace any non-matching
          // attribute with the same name.
          var iter = node.axisIterator(Axis.ATTRIBUTE)
          while (iter.hasNext) {
            val child = iter.next.asInstanceOf[XdmNode]
            if (!matches(child)) {
              traverse(child)
            }
          }

          iter = node.axisIterator(Axis.ATTRIBUTE)
          while (iter.hasNext) {
            val child = iter.next.asInstanceOf[XdmNode]
            if (matches(child)) {
              traverse(child)
            }
          }

          receiver.startContent()
        }

        if (!nmatch || processChildren) {
          traverseChildren(node)
        }

        if (nmatch) {
          processor.endElement(node)
        } else {
          addEndElement()
        }

      case XdmNodeKind.ATTRIBUTE =>
        // FIXME: what about changing the name of an attribute?
        if (nmatch) {
          processor.attribute(node)
          saw = 0
        } else {
          addAttribute(node.getNodeName, node.getStringValue)
        }

      case XdmNodeKind.COMMENT =>
        if (nmatch) {
          processor.comment(node)
          saw = 0
        } else {
          addComment(node.getStringValue)
        }

      case XdmNodeKind.TEXT =>
        if (nmatch) {
          processor.text(node)
          saw = 0
        } else {
          addText(node.getStringValue)
        }

      case XdmNodeKind.PROCESSING_INSTRUCTION =>
        if (nmatch) {
          processor.pi(node)
          saw = 0
        } else {
          addPI(node.getNodeName.getLocalName, node.getStringValue)
        }

      case _ => throw new UnsupportedOperationException(s"Unexpected node type: $node")
    }
  }

  private def traverse(node: XdmNode, deep: Boolean): Unit = {
    val nmatch = matches(node)

    if (nmatch) {
      nodeCount += 1
    }

    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        if (!nmatch || deep) {
          traverseDeepChildren(node, deep, Axis.CHILD)
        }
      case XdmNodeKind.ELEMENT =>
        if (!nmatch || deep) {
          traverseDeepChildren(node, deep, Axis.ATTRIBUTE)
          traverseDeepChildren(node, deep, Axis.CHILD)
        }
      case _ => Unit
    }
  }

  private def traverseChildren(node: XdmNode): Unit = {
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next.asInstanceOf[XdmNode]
      traverse(child)
    }
  }

  private def traverseDeepChildren(node: XdmNode, deep: Boolean, axis: Axis): Unit = {
    val iter = node.axisIterator(axis)
    while (iter.hasNext) {
      val child = iter.next.asInstanceOf[XdmNode]
      traverse(child, deep)
    }
  }

  private def nsBindings(node: XdmNode): Map[String,String] = {
    var bindings = HashMap.empty[String,String]
    val nsIter = node.axisIterator(Axis.NAMESPACE)
    while (nsIter.hasNext) {
      val ns = nsIter.next.asInstanceOf[XdmNode]
      val nodeName = ns.getNodeName
      val uri = ns.getStringValue
      if (nodeName == null) {
        // Huh?
        bindings += ("" -> uri)
      } else {
        bindings += (nodeName.getLocalName -> uri)
      }
    }
    bindings.toMap
  }

  private class MatchingNamespaceResolver(bindings: Map[String, String]) extends NamespaceResolver {
    private val ns = HashMap.empty[String,String]

    override def getURIForPrefix(prefix: String, useDefault: Boolean): String = {
      if ("" == prefix && !useDefault) {
        return ""
      }

      ns(prefix)
    }

    override def iteratePrefixes(): util.Iterator[String] = {
      val p = ListBuffer.empty[String]
      for (pfx <- ns.keySet) {
        p += pfx
      }
      p.iterator.asJava
    }
  }
}
