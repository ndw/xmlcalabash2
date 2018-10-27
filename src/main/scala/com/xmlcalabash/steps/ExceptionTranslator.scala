package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.`type`.ValidationException
import net.sf.saxon.s9api.QName
import net.sf.saxon.trans.XPathException
import org.xml.sax.SAXParseException

class ExceptionTranslator() extends DefaultXmlStep {
  private val _uri = new QName("uri")
  private val _line = new QName("line")
  private val _column = new QName("column")
  private val _xpath = new QName("xpath")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    val tree = new SaxonTreeBuilder(config)
    tree.startDocument(None)
    tree.addStartElement(XProcConstants.c_errors)
    tree.addNamespace("cx", XProcConstants.ns_cx)
    tree.startContent()

    item match {
      case except: Throwable =>
        formatException(tree, except)
      case _ =>
        throw new RuntimeException(s"Unexpected input to exception translator: $item not an exception!")
    }

    tree.addEndElement()
    tree.endDocument()
    consumer.get.receive("result", tree.result, new XProcMetadata(MediaType.XML))
  }

  private def formatException(tree: SaxonTreeBuilder, exception: Throwable): Unit = {
    exception match {
      case cause: XProcException =>
        formatXProcException(tree, cause)
      case cause: ValidationException =>
        formatValidationException(tree, cause)
      case cause: XPathException =>
        formatXPathException(tree, cause)
      case cause: SAXParseException =>
        formatSAXParseException(tree, cause)
      case _ =>
        formatGenericException(tree, exception)
    }
  }

  private def formatGenericException(tree: SaxonTreeBuilder, exception: Throwable): Unit = {
    tree.addStartElement(XProcConstants.c_error)
    tree.addAttribute(XProcConstants.cx_class, exception.getClass.getName)
    tree.startContent()
    tree.addText(exception.getMessage)
    tree.addEndElement()
  }

  private def formatXProcException(tree: SaxonTreeBuilder, exception: XProcException): Unit = {
    tree.addStartElement(XProcConstants.c_error)
    tree.addAttribute(XProcConstants._code, exception.code.toString)
    if (exception.location.isDefined) {
      val loc = exception.location.get
      if (loc.uri.isDefined) {
        tree.addAttribute(_uri, loc.uri.get)
      }
      if (loc.line.isDefined) {
        tree.addAttribute(_line, loc.line.get.toString)
      }
      if (loc.column.isDefined) {
        tree.addAttribute(_column, loc.column.get.toString)
      }
    }
    tree.startContent()
    tree.addText(exception.getMessage)
    tree.addEndElement()

    for (cause <- exception.underlyingCauses) {
      formatException(tree, cause)
    }
  }

  private def formatXPathException(tree: SaxonTreeBuilder, exception: XPathException): Unit = {
    if (exception.getException == null) {
      tree.addStartElement(XProcConstants.c_error)
      tree.startContent()
      tree.addText(exception.getMessage)
      tree.addEndElement()
    } else {
      formatException(tree, exception.getException)
    }
  }

  private def formatSAXParseException(tree: SaxonTreeBuilder, exception: SAXParseException): Unit = {
    tree.addStartElement(XProcConstants.c_error)
    tree.addAttribute(_uri, exception.getSystemId)
    tree.addAttribute(_line, exception.getLineNumber.toString)
    tree.addAttribute(_column, exception.getColumnNumber.toString)
    tree.startContent()
    tree.addText(exception.getMessage)
    tree.addEndElement()
  }

  private def formatValidationException(tree: SaxonTreeBuilder, exception: ValidationException): Unit = {
    val vf = exception.getValidationFailure

    tree.addStartElement(XProcConstants.c_error)
    tree.addAttribute(XProcConstants._code, vf.getErrorCode)
    tree.addAttribute(_uri, vf.getSystemId)
    tree.addAttribute(_xpath, exception.getPath)
    tree.addAttribute(_line, vf.getLineNumber.toString)
    tree.addAttribute(_column, vf.getColumnNumber.toString)
    tree.startContent()
    tree.addText(vf.getMessage)
    tree.addEndElement()
  }
}
