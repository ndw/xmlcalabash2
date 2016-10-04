package com.xmlcalabash.model.xml.util

import com.xmlcalabash.core.{XProcConstants, XProcException}
import net.sf.saxon.s9api._
import net.sf.saxon.trans.XPathException

import scala.collection.mutable.ArrayBuffer

/**
  * Created by ndw on 6/21/15.
  */
object RelevantNodes {
  def filter(start: XdmNode, axis: Axis): Iterator[XdmItem] = {
    val rn = new RNodes(start, axis)
    rn.filter()
  }

  def filter(start: XdmNode, axis: Axis, name: QName): Iterator[XdmItem] = {
    val rn = new RNodes(start, axis, name)
    rn.filter()
  }

  def filter(start: XdmNode, axis: Axis, ignore: Boolean): Iterator[XdmItem] = {
    val rn = new RNodes(start, axis, ignore)
    rn.filter()
  }

  private class RNodes(start: XdmNode, val axis: Axis, val ignoreInfo: Boolean, val onlyMatch: Option[QName]) {
    val processor = start.getUnderlyingNode.getConfiguration.getProcessor.asInstanceOf[Processor]

    def this(start: XdmNode, axis: Axis) {
      this(start, axis, true, None)
    }

    def this(start: XdmNode, axis: Axis, name: QName) {
      this(start, axis, true, Some(name))
    }

    def this(start: XdmNode, axis: Axis, ignore: Boolean) {
      this(start, axis, ignore, None)
    }

    def filter(): Iterator[XdmItem] = {
      val iter = start.axisIterator(axis)
      // FIXME: make this efficient
      // iter.filter(relevant(_)) used to work in some earlier version of Scala
      val buf = ArrayBuffer.empty[XdmItem]
      while (iter.hasNext) {
        val item = iter.next()
        if (relevant(item)) {
          buf += item
        }
      }
      buf.iterator
    }

    private def relevant(nodev: XdmValue): Boolean = {
      val node = nodev.asInstanceOf[XdmNode]

      if (ignoreInfo
        && ((XProcConstants.p_documentation == node.getNodeName) || (XProcConstants.p_pipeinfo == node.getNodeName))) {
        return false
      }

      if ((node.getNodeKind eq XdmNodeKind.COMMENT) || (node.getNodeKind eq XdmNodeKind.PROCESSING_INSTRUCTION)) {
        return false
      }

      if (node.getNodeKind eq XdmNodeKind.TEXT) {
        return !("" == node.toString.trim)
      }

      if (node.getNodeKind eq XdmNodeKind.ELEMENT) {
        if (((XProcConstants.NS_XPROC == node.getNodeName.getNamespaceURI) && node.getAttributeValue(XProcConstants._use_when) != null)
          || (!(XProcConstants.NS_XPROC == node.getNodeName.getNamespaceURI) && node.getAttributeValue(XProcConstants.p_use_when) != null)) {
          var expr = node.getAttributeValue(XProcConstants._use_when)
          if (!(XProcConstants.NS_XPROC == node.getNodeName.getNamespaceURI)) {
            expr = node.getAttributeValue(XProcConstants.p_use_when)
          }
          return useWhen(node, expr)
        } else {
          return onlyMatch.isEmpty || (onlyMatch.get eq node.getNodeName)
        }
      }

      if (node.getNodeKind eq XdmNodeKind.ATTRIBUTE) {
        return onlyMatch.isEmpty || (onlyMatch.get eq node.getNodeName)
      }

      if (node.getNodeKind eq XdmNodeKind.NAMESPACE) {
        return true
      }

      false
    }

    private def useWhen(element: XdmNode, xpath: String): Boolean = {
      var use: Boolean = false

      try {
        var xcomp = processor.newXPathCompiler
        var nsIter = element.axisIterator(Axis.NAMESPACE)
        while (nsIter.hasNext) {
          var ns = nsIter.next.asInstanceOf[XdmNode]
          xcomp.declareNamespace(ns.getNodeName.getLocalName, ns.getStringValue)
        }

        var xexec: XPathExecutable = null

        try {
          xexec = xcomp.compile(xpath)
        } catch {
          case sae: SaxonApiException => {
            throw sae
          }
        }

        var selector: XPathSelector = xexec.load

        try {
          use = selector.effectiveBooleanValue()
        } catch {
          case saue: SaxonApiUncheckedException =>
            val sae = saue.getCause
            sae match {
              case exception: XPathException =>
                var xe: XPathException = exception
                if (("http://www.w3.org/2005/xqt-errors" == xe.getErrorCodeNamespace) && ("XPDY0002" == xe.getErrorCodeLocalPart)) {
                  throw new XProcException("Expression refers to context when none is available")
                  // FIXME: throw XProcException.dynamicError(26, element, "Expression refers to context when none is available: " + xpath)
                } else {
                  throw saue
                }
              case _ =>
                throw saue
            }
        }
      } catch {
        case sae: Throwable =>
          if (sae.isInstanceOf[XPathException]) {
            throw new XProcException(sae.getCause.getMessage)
            // FIXME: throw XProcException.dynamicError(23, element, sae.getCause.getMessage)
          } else {
            throw new XProcException(sae.getCause.getMessage)
            // FIXME: throw new XProcException(sae)
          }
      }

      use
    }
  }
}
