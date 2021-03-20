package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmNode}

class Error extends DefaultXmlStep {
  private val _code = new QName("", "code")

  private var _message = Option.empty[XdmNode]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode => _message = Some(node)
      case _ => logger.debug(s"p:error received unexpected item: $item")
    }
  }

  override def run(context: StaticContext): Unit = {
    val code = qnameBinding(_code).get

    val tree = new SaxonTreeBuilder(config)
    if (_message.isDefined) {
      tree.startDocument(_message.get.getBaseURI)
    } else {
      tree.startDocument(context.baseURI)
    }
    tree.addStartElement(XProcConstants.c_errors)
    tree.startContent()
    tree.addStartElement(XProcConstants.c_error)
    tree.addAttribute(XProcConstants._code, code.toString)
    if (context.location.isDefined) {
      if (context.location.get.uri.isDefined) {
        tree.addAttribute(XProcConstants._href, context.location.get.uri.get.toString)
      }
      if (context.location.get.line.isDefined) {
        tree.addAttribute(XProcConstants._line, context.location.get.line.get.toString)
      }
      if (context.location.get.column.isDefined) {
        tree.addAttribute(XProcConstants._column, context.location.get.column.get.toString)
      }
    }
    tree.addAttribute(XProcConstants._type, "p:error")
    tree.addNamespace("p", XProcConstants.ns_p)
    tree.startContent()
    if (_message.isDefined) {
      tree.addSubtree(_message.get)
    }
    tree.addEndElement()
    tree.addEndElement()
    tree.endDocument()

    throw XProcException.xcGeneralException(code, Some(tree.result), context.location)
  }
}
