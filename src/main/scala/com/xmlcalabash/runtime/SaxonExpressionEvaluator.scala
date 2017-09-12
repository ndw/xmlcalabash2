package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.ExpressionEvaluator
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.util.{SaxonTreeBuilder, StringParsers}
import com.xmlcalabash.model.xml.XProcConstants
import net.sf.saxon.s9api.{QName, SaxonApiException, SaxonApiUncheckedException, XPathExecutable, XdmAtomicValue, XdmItem, XdmNode, XdmNodeKind}
import net.sf.saxon.trans.XPathException
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.DynamicVariable

// N.B. The evaluator must be reentrant because there can be only one instance of it because
// it has a dynamic variable used to pass context to extension functions.
class SaxonExpressionEvaluator(xmlCalabash: XMLCalabash) extends ExpressionEvaluator {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val _dynContext = new DynamicVariable[DynamicContext](null)

  def withContext[T](context: DynamicContext)(thunk: => T): T = _dynContext.withValue(context)(thunk)
  def dynContext: Option[DynamicContext] = Option(_dynContext.value)

  override def newInstance(): SaxonExpressionEvaluator = {
    this
  }

  override def value(xpath: Any, context: List[Message], bindings: Map[String, Message]): Any = {
    val proxies = mutable.HashMap.empty[Any, XdmNode]
    val newContext = new DynamicContext()
    if (context.nonEmpty) {
      context.head match {
        case item: ItemMessage =>
          val node = proxy(item)
          proxies.put(item.item, node)
          checkDocument(newContext, node, context.head)
        case _ => Unit
      }
    }

    for ((str, value) <- bindings) {
      value match {
        case bind: BindingMessage =>
          bind.message match {
            case item: ItemMessage =>
              item.item match {
                case xitem: XdmNode =>
                  checkDocument(newContext, xitem, context.head)
                case _ => Unit
              }
            case _ => throw new PipelineException("unexpected", "Unexpected message in binding: " + bind.message, None)
          }
        case _ => throw new PipelineException("unexpected", "Unexpected binding message: " + value, None)
      }
    }

    withContext(newContext) { do_value(xpath, context, bindings, proxies.toMap) }
  }

  override def booleanValue(xpath: Any, context: List[Message], bindings: Map[String, Message]): Boolean = {
    val proxies = mutable.HashMap.empty[Any, XdmNode]
    val newContext = new DynamicContext()
    if (context.nonEmpty) {
      context.head match {
        case item: ItemMessage =>
          val node = proxy(item)
          proxies.put(item.item, node)
          checkDocument(newContext, node, context.head)
        case _ => Unit
      }
    }

    for ((str, value) <- bindings) {
      value match {
        case bind: BindingMessage =>
          bind.message match {
            case item: ItemMessage =>
              item.item match {
                case xitem: XdmNode =>
                  checkDocument(newContext, xitem, context.head)
                case _ => Unit
              }
            case _ => throw new PipelineException("unexpected", "Unexpected message in binding: " + bind.message, None)
          }
        case _ => throw new PipelineException("unexpected", "Unexpected binding message: " + value, None)
      }
    }


    val item = withContext(newContext) { do_value(xpath, context, bindings, proxies.toMap) }
    val result = item match {
      case atomic: XdmAtomicValue =>
        atomic.getBooleanValue
      case _ => true
    }

    result
  }

  def do_value(xpath: Any, context: List[Message], bindings: Map[String, Message], proxies: Map[Any,XdmNode]): XdmItem = {
    xpath match {
      case expr: XProcExpression =>
        val patchBindings = mutable.HashMap.empty[QName, XdmItem]

        for ((str, value) <- bindings) {
          value match {
            case bind: BindingMessage =>
              bind.message match {
                case item: ItemMessage =>
                  item.item match {
                    case xitem: XdmItem =>
                      patchBindings.put(StringParsers.parseClarkName(str), xitem)
                    case _ =>
                      throw new PipelineException("unexpected", "Bound value is not XdmItem: " + item.item, None)
                  }
                case _ => throw new PipelineException("unexpected", "Unexpected message in binding: " + bind.message, None)
              }
            case _ => throw new PipelineException("unexpected", "Unexpected binding message: " + value, None)
          }
        }

        val result = value(expr, context, patchBindings.toMap, proxies)
        result
      case str: String =>
        new XdmAtomicValue(str)
      case _ =>
        logger.warn("Unexpected expression type, returning string value: " + xpath)
        new XdmAtomicValue(xpath.toString)
    }
  }

  def value(xpath: XProcExpression, context: List[Message], bindings: Map[QName, XdmItem], proxies: Map[Any,XdmNode]): XdmItem = {
    var result = ListBuffer.empty[XdmItem]
    xpath match {
      case avtexpr: XProcAvtExpression =>
        var evalAvt = false
        for (part <- avtexpr.avt) {
          if (evalAvt) {
            val epart = computeValue(part, context, avtexpr.nsbindings, bindings, proxies, avtexpr.extensionFunctionsAllowed)
            for (item <- epart.asInstanceOf[ListBuffer[XdmItem]]) {
              result += item
            }
          } else {
            if (part != "") {
              result += new XdmAtomicValue(part)
            }
          }
          evalAvt = !evalAvt
        }
      case xpathexpr: XProcXPathExpression =>
        val epart = computeValue(xpathexpr.expr, context, xpathexpr.nsbindings, bindings, proxies, xpathexpr.extensionFunctionsAllowed)
        for (item <- epart.asInstanceOf[ListBuffer[XdmItem]]) {
          result += item
        }
      case _ => throw new PipelineException("unexpected", "Unexpected type passed to value", None)
    }

    if (result.size == 1) {
      result.head
    } else {
      throw new PipelineException("unimpl", "Support for sequence results not yet implemented", None)
    }
  }

  private def computeValue(xpath: String,
                           context: List[Message],
                           nsbindings: Map[String, String],
                           bindings: Map[QName,XdmItem],
                           proxies: Map[Any, XdmNode],
                           extensionsOk: Boolean): Any = {
    val results = ListBuffer.empty[XdmItem]
    val config = xmlCalabash.processor.getUnderlyingConfiguration

    if (context.size > 1) {
      throw new PipelineException("seq", "Sequence not allowed as context for expression", None)
    }

    try {
      val xcomp = xmlCalabash.processor.newXPathCompiler()
      val baseURI: URI = new URI("") // FIXME: get baseURI from dynamic context
      if (baseURI.toASCIIString != "") {
        xcomp.setBaseURI(baseURI)
      }

      if (extensionsOk) {
        throw new PipelineException("notimpl", "Extension functions aren't implemented yet", None)
      }

      for (varname <- bindings.keySet) {
        xcomp.declareVariable(varname)
      }

      for ((prefix, uri) <- nsbindings) {
        xcomp.declareNamespace(prefix, uri)
      }

      var xexec: XPathExecutable = null // Yes, I know.
      try {
        xexec = xcomp.compile(xpath)
      } catch {
        case sae: SaxonApiException =>
          sae.getCause match {
            case xpe: XPathException =>
              if (xpe.getMessage.contains("Undeclared variable")) {
                throw new PipelineException("undecl", xpe.getMessage, None)
              } else {
                throw sae
              }
            case _ => throw sae
          }
        case other: Throwable =>
          throw other
      }

      val selector = xexec.load()

      for ((varname, varvalue) <- bindings) {
        // FIXME: parse Clark names
        selector.setVariable(varname, varvalue)
      }

      if (context.nonEmpty) {
        context.head match {
          case item: ItemMessage =>
            selector.setContextItem(proxies(item.item))
        }
      }

      try {
        val values = selector.iterator()
        while (values.hasNext) {
          results += values.next()
        }
      } catch {
        case saue: SaxonApiUncheckedException =>
          saue.getCause match {
            case xpe: XPathException =>
              if ((xpe.getErrorCodeNamespace == "http://www.w3.org/2005/xqt-errors")
                   && xpe.getErrorCodeLocalPart == "XPDY0002") {
                throw new PipelineException("nocontext","Expression refers to context when none is available: " + xpath, None)
              } else {
                throw saue
              }
            case _ => throw saue
          }
        case other: Throwable =>
          throw other
      }


    } catch {
      case sae: SaxonApiException => throw sae
      case other: Throwable => throw other

    }

    results
  }

  def checkDocument(dynContext: DynamicContext, node: XdmNode, msg: Message): Unit = {
    var p: XdmNode = node
    while (Option(p.getParent).isDefined) {
      p = p.getParent
    }

    if (p.getNodeKind == XdmNodeKind.DOCUMENT) {
      dynContext.addDocument(p.getUnderlyingNode, msg)
    }
  }

  private def proxy(message: Message): XdmNode = {
    message match {
      case item: ItemMessage =>
        if (item.item.isInstanceOf[XdmNode]) {
          return item.item.asInstanceOf[XdmNode]
        }

        item.metadata match {
          case xproc: XProcMetadata =>
            val props = xproc.properties
            val builder = new SaxonTreeBuilder(xmlCalabash)
            builder.startDocument(None)
            builder.addStartElement(XProcConstants.c_document_properties)
            builder.startContent()
            for ((key,value) <- props) {
              builder.addStartElement(XProcConstants.c_property)
              builder.addAttribute(XProcConstants._name, key)
              builder.addAttribute(XProcConstants._value, value)
              builder.startContent()
              builder.addEndElement()
            }
            builder.addEndElement()
            builder.endDocument()
            builder.result
          case _ => emptyProxy()
        }
      case _ => emptyProxy()
    }
  }

  private def emptyProxy(): XdmNode = {
    val builder = new SaxonTreeBuilder(xmlCalabash)
    builder.startDocument(None)
    builder.addStartElement(XProcConstants.c_document_properties)
    builder.startContent()
    builder.addEndElement()
    builder.endDocument()
    builder.result
  }

}
