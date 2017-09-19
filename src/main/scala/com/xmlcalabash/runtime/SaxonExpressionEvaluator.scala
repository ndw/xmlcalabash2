package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.ExpressionEvaluator
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{StepException, XProcException}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.util.XProcVarValue
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

  override def value(xpath: Any, context: List[Message], bindings: Map[String, Message]): XPathItemMessage = {
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

    xpath match {
      case xpath: XProcXPathExpression =>
        if (xpath.context.location.isDefined) {
          newContext.location = xpath.context.location.get
        }
      case xpath: XProcAvtExpression =>
        if (xpath.context.location.isDefined) {
          newContext.location = xpath.context.location.get
        }
      case _ => println("DIDN'T MATCH: " + xpath)
    }

    for ((str, value) <- bindings) {
      value match {
        case item: ItemMessage =>
          item.item match {
            case xitem: XdmNode =>
              checkDocument(newContext, xitem, value)
            case _ => Unit
          }
        case _ =>
          throw XProcException.xiInvalidMessage(newContext.location, value)
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

    xpath match {
      case xpath: XProcXPathExpression =>
        if (xpath.context.location.isDefined) {
          newContext.location = xpath.context.location.get
        }
      case xpath: XProcAvtExpression =>
        if (xpath.context.location.isDefined) {
          newContext.location = xpath.context.location.get
        }
      case _ => println("DIDN'T MATCH: " + xpath)
    }

    for ((str, value) <- bindings) {
      value match {
        case item: ItemMessage =>
          item.item match {
            case xitem: XdmNode =>
              checkDocument(newContext, xitem, value)
            case _ => Unit
          }
        case _ =>
          throw XProcException.xiInvalidMessage(newContext.location, value)
      }
    }

    val item = withContext(newContext) { do_value(xpath, context, bindings, proxies.toMap) }
    val result = item.item match {
      case atomic: XdmAtomicValue =>
        atomic.getBooleanValue
      case _ => true
    }

    result
  }

  def do_value(xpath: Any, context: List[Message], bindings: Map[String, Message], proxies: Map[Any,XdmNode]): XPathItemMessage = {
    xpath match {
      case expr: XProcExpression =>
        val patchBindings = mutable.HashMap.empty[QName, XdmItem]

        for ((str, value) <- bindings) {
          value match {
            case item: ItemMessage =>
              item.item match {
                case xitem: XdmItem =>
                  patchBindings.put(ValueParser.parseClarkName(str), xitem)
                case value: XProcVarValue =>
                  patchBindings.put(ValueParser.parseClarkName(str), value.value)
                case _ =>
                  throw XProcException.xiBadBoundValue(expr.context.location, item.item)
              }
            case _ =>
              throw XProcException.xiInvalidMessage(None, value)
          }
        }

        val result = value(expr, context, patchBindings.toMap, proxies)
        new XPathItemMessage(result, XProcMetadata.ANY, expr.context)
      case str: String =>
        new XPathItemMessage(new XdmAtomicValue(str), XProcMetadata.ANY, ExpressionContext.NONE)
      case _ =>
        logger.warn("Unexpected expression type, returning string value: " + xpath)
        new XPathItemMessage(new XdmAtomicValue(xpath.toString), XProcMetadata.ANY, ExpressionContext.NONE)
    }
  }

  def value(xpath: XProcExpression, context: List[Message], bindings: Map[QName, XdmItem], proxies: Map[Any,XdmNode]): XdmItem = {
    var result = ListBuffer.empty[XdmItem]
    xpath match {
      case avtexpr: XProcAvtExpression =>
        var evalAvt = false
        for (part <- avtexpr.avt) {
          if (evalAvt) {
            val epart = computeValue(part, context, avtexpr.context, bindings, proxies, avtexpr.extensionFunctionsAllowed)
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
        val epart = computeValue(xpathexpr.expr, context, xpathexpr.context, bindings, proxies, xpathexpr.extensionFunctionsAllowed)
        for (item <- epart.asInstanceOf[ListBuffer[XdmItem]]) {
          result += item
        }
      case _ =>
        throw XProcException.xiUnexpectedExprType(xpath.context.location, xpath)
    }

    if (result.size == 1) {
      result.head
    } else {
      throw XProcException.xiSeqNotSupported(xpath.context.location, xpath)
    }
  }

  private def computeValue(xpath: String,
                           contextItem: List[Message],
                           exprContext: ExpressionContext,
                           bindings: Map[QName,XdmItem],
                           proxies: Map[Any, XdmNode],
                           extensionsOk: Boolean): Any = {
    val results = ListBuffer.empty[XdmItem]
    val config = xmlCalabash.processor.getUnderlyingConfiguration

    if (contextItem.size > 1) {
      throw XProcException.dynamicError(5, exprContext.location)
    }

    try {
      val xcomp = xmlCalabash.processor.newXPathCompiler()
      val baseURI = if (exprContext.baseURI.isDefined) {
        exprContext.baseURI.get
      } else {
        new URI("")
      }
      if (baseURI.toASCIIString != "") {
        xcomp.setBaseURI(baseURI)
      }

      for (varname <- bindings.keySet) {
        xcomp.declareVariable(varname)
      }

      for ((prefix, uri) <- exprContext.nsBindings) {
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

      if (contextItem.nonEmpty) {
        contextItem.head match {
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
              val code = new QName(xpe.getErrorCodeNamespace, xpe.getErrorCodeLocalPart)
              throw new StepException(code, xpe.getMessage, xpe, exprContext.location)
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
        item.item match {
          case node: XdmNode => return node
          case _ => Unit
        }

        item.metadata match {
          case xproc: XProcMetadata =>
            val props = xproc.properties
            val builder = new SaxonTreeBuilder(xmlCalabash)
            builder.startDocument(None)
            builder.addStartElement(XProcConstants.c_document_properties)
            builder.startContent()
            for ((key,value) <- props) {
              builder.addStartElement(key)
              builder.startContent()
              value match {
                case atom: XdmAtomicValue => builder.addText(value.getStringValue)
                case node: XdmNode => builder.addSubtree(node)
                case _ => throw new RuntimeException("Huh?")
              }
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
