package com.xmlcalabash.runtime

import java.net.URI
import java.util

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.ExpressionEvaluator
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{StepException, XProcException}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.util.XProcVarValue
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.{CollectionFinder, Resource, ResourceCollection}
import net.sf.saxon.om.{Item, SpaceStrippingRule}
import net.sf.saxon.s9api.{QName, SaxonApiException, SaxonApiUncheckedException, XPathExecutable, XdmAtomicValue, XdmItem, XdmNode, XdmNodeKind, XdmValue}
import net.sf.saxon.trans.XPathException
import net.sf.saxon.value.SequenceType
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.DynamicVariable

object SaxonExpressionEvaluator {
  val DEFAULT_COLLECTION = "https://xmlcalabash.com/default-collection"
  protected val _dynContext = new DynamicVariable[DynamicContext](null)
}

class SaxonExpressionEvaluator(xmlCalabash: XMLCalabash) extends ExpressionEvaluator {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def withContext[T](context: DynamicContext)(thunk: => T): T = SaxonExpressionEvaluator._dynContext.withValue(context)(thunk)
  def dynContext: Option[DynamicContext] = Option(SaxonExpressionEvaluator._dynContext.value)

  override def newInstance(): SaxonExpressionEvaluator = {
    new SaxonExpressionEvaluator(xmlCalabash)
  }

  override def singletonValue(xpath: Any, context: List[Message], bindings: Map[String, Message], options: Option[Any]): XPathItemMessage = {
    val msgs = value(xpath, context, bindings, options)
    if (msgs.length == 1) {
      msgs.head
    } else {
      xpath match {
        case xpath: XProcXPathExpression =>
          throw XProcException.xiSeqNotSupported(xpath.context.location, xpath)
        case xpath: XProcAvtExpression =>
          var s = ""
          for (item <- msgs) {
            s += item.item.getStringValue
          }
          new XPathItemMessage(new XdmAtomicValue(s), XProcMetadata.ANY, ExpressionContext.NONE)
        // We should never fall off the end of this match because we got past the value() call above
      }
    }
  }

  override def value(xpath: Any, context: List[Message], bindings: Map[String, Message], options: Option[Any]): List[XPathItemMessage] = {
    val proxies = mutable.HashMap.empty[Any, XdmItem]
    val newContext = new DynamicContext()
    for (msg <- context) {
      msg match {
        case item: ItemMessage =>
          val node = proxy(item)
          proxies.put(item.item, node)
          checkDocument(newContext, node, context.head)
        case _ => Unit
      }
    }

    if (options.isDefined) {
      options.get match {
        case seo: SaxonExpressionOptions =>
          if (seo.inj_elapsed.isDefined) {
            newContext.injElapsed = seo.inj_elapsed.get
          }
          if (seo.inj_id.isDefined) {
            newContext.injId = seo.inj_id.get
          }
          if (seo.inj_name.isDefined) {
            newContext.injName = seo.inj_name.get
          }
          if (seo.inj_type.isDefined) {
            newContext.injType = seo.inj_type.get
          }
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
      case _ =>
        throw XProcException.xiNotAnXPathExpression(xpath, None)
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

    withContext(newContext) { compute(xpath.asInstanceOf[XProcExpression], context, bindings, proxies.toMap, options) }
  }

  override def booleanValue(xpath: Any, context: List[Message], bindings: Map[String, Message], options: Option[Any]): Boolean = {
    val msg = singletonValue(xpath, context, bindings, options)

    msg.item match {
      case atomic: XdmAtomicValue => atomic.getBooleanValue
      case _ => true
    }
  }

  def compute(xpath: XProcExpression, context: List[Message], bindings: Map[String, Message], proxies: Map[Any,XdmItem], options: Option[Any]): List[XPathItemMessage] = {
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
              throw XProcException.xiBadBoundValue(xpath.context.location, item.item)
          }
        case _ =>
          throw XProcException.xiInvalidMessage(None, value)
      }
    }

    val result = ListBuffer.empty[XdmItem]
    xpath match {
      case avtexpr: XProcAvtExpression =>
        var evalAvt = false
        for (part <- avtexpr.avt) {
          if (evalAvt) {
            val epart = computeValue(part, None, context, avtexpr.context, patchBindings.toMap, proxies, avtexpr.extensionFunctionsAllowed, options)
            for (item <- epart) {
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
        val epart = computeValue(xpathexpr.expr, xpathexpr.as, context, xpathexpr.context, patchBindings.toMap, proxies, xpathexpr.extensionFunctionsAllowed, options)
        for (item <- epart) {
          result += item
        }
      case _ =>
        throw XProcException.xiUnexpectedExprType(xpath.context.location, xpath)
    }

    val messages = ListBuffer.empty[XPathItemMessage]
    for (value <- result) {
      value match {
        case node: XdmNode => messages += new XPathItemMessage(value, XProcMetadata.XML, xpath.context)
        case _ => messages += new XPathItemMessage(value, XProcMetadata.ANY, xpath.context)
      }
    }

    messages.toList
  }

  private def computeValue(xpath: String,
                           as: Option[SequenceType],
                           contextItem: List[Message],
                           exprContext: ExpressionContext,
                           bindings: Map[QName,XdmItem],
                           proxies: Map[Any, XdmItem],
                           extensionsOk: Boolean,
                           options: Option[Any]): List[XdmItem] = {
    val results = ListBuffer.empty[XdmItem]
    val config = xmlCalabash.processor.getUnderlyingConfiguration
    val collection = List.empty[XdmNode]

    val useCollection = if (options.isDefined) {
      options.get match {
        case seo: SaxonExpressionOptions => seo.contextCollection.getOrElse(false)
        case _ => false
      }
    } else {
      false
    }

    if (contextItem.size > 1) {
      if (!useCollection) {
        throw XProcException.dynamicError(5, exprContext.location)
      }
    }

    val sconfig = xmlCalabash.processor.getUnderlyingConfiguration
    val curfinder = sconfig.getCollectionFinder
    sconfig.setCollectionFinder(new ExprCollectionFinder(curfinder, contextItem))
    sconfig.setDefaultCollection(SaxonExpressionEvaluator.DEFAULT_COLLECTION)

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

      var value: XdmValue = null
      try {
        value = selector.evaluate()
      } catch {
        case saue: SaxonApiUncheckedException =>
          saue.getCause match {
            case xpe: XPathException =>
              val code = new QName(xpe.getErrorCodeNamespace, xpe.getErrorCodeLocalPart)
              throw new StepException(code, xpe.getMessage, xpe, exprContext.location)
            case _ => throw saue
          }
        case sae: SaxonApiException =>
          sae.getCause match {
            case xpe: XPathException =>
              val code = new QName(xpe.getErrorCodeNamespace, xpe.getErrorCodeLocalPart)
              throw new StepException(code, xpe.getMessage, xpe, exprContext.location)
            case _ => throw sae
          }
        case other: Throwable =>
          throw other
      }

      if (as.isDefined) {
        val matches = as.get.matches(value.getUnderlyingValue, config.getTypeHierarchy)
        if (!matches) {
          throw XProcException.dynamicError(36, List(value, as.get), exprContext.location)
        }
      }

      val values = value.iterator()
      while (values.hasNext) {
        results += values.next()
      }
    } catch {
      case sae: SaxonApiException => throw sae
      case other: Throwable => throw other
    }

    results.toList
  }

  // =========================================================================================================

  def checkDocument(dynContext: DynamicContext, item: XdmItem, msg: Message): Unit = {
    item match {
      case node: XdmNode =>
        var p: XdmNode = node
        while (Option(p.getParent).isDefined) {
          p = p.getParent
        }

        if (p.getNodeKind == XdmNodeKind.DOCUMENT) {
          dynContext.addDocument(p, msg)
        }
      case _ => Unit
    }
  }

  private def proxy(message: Message): XdmItem = {
    message match {
      case item: ItemMessage =>
        item.item match {
          case node: XdmNode => return node
          case item: XdmItem => return item
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
                case _ => throw XProcException.xiInvalidPropertyValue(value, None)
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

  class ExprCollectionFinder(finder: CollectionFinder, messages: List[Message]) extends CollectionFinder {
    private val items = ListBuffer.empty[ExprNodeResource]

    for (msg <- messages) {
      val node = dynContext.get.document(msg)
      if (node.isDefined) {
        items += new ExprNodeResource(node.get)
      } else {
        msg match {
          case item: XPathItemMessage =>
            items += new ExprNodeResource(item.item.asInstanceOf[XdmNode])
          case item: ItemMessage =>
            item.item match {
              case node: XdmNode => items += new ExprNodeResource(node)
              case _ => Unit
            }
          case _ => throw XProcException.xiBadMessage(msg, None)
        }
      }
    }
    private val rsrcColl = new ExprResourceCollection(items.toList)

    override def findCollection(context: XPathContext, collectionURI: String): ResourceCollection = {
      if (collectionURI == SaxonExpressionEvaluator.DEFAULT_COLLECTION) {
        rsrcColl
      } else {
        finder.findCollection(context, collectionURI)
      }
    }
  }

  class ExprResourceCollection(items: List[ExprNodeResource]) extends ResourceCollection {
    override def getCollectionURI: String = ""

    override def getResourceURIs(context: XPathContext): util.Iterator[String] = {
      val uris = ListBuffer.empty[String]
      for (rsrc <- items) {
        uris += rsrc.getResourceURI
      }
      uris.iterator.asJava
    }

    override def getResources(context: XPathContext): util.Iterator[_ <: Resource] = {
      items.iterator.asJava
    }

    override def stripWhitespace(rules: SpaceStrippingRule): Boolean = {
      false
    }

    override def isStable(context: XPathContext): Boolean = {
      true
    }
  }

  class ExprNodeResource(node: XdmNode) extends Resource {
    override def getResourceURI: String = node.getBaseURI.toASCIIString
    override def getItem(context: XPathContext): Item = node.getUnderlyingNode.head
    override def getContentType: String = null
  }
}
