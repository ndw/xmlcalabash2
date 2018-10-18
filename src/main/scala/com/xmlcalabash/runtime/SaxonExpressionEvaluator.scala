package com.xmlcalabash.runtime

import java.net.URI
import java.util

import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.ExpressionEvaluator
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{StepException, XProcException}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.util.{MediaType, XProcVarValue}
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.{CollectionFinder, Resource, ResourceCollection}
import net.sf.saxon.om.{Item, SpaceStrippingRule}
import net.sf.saxon.s9api.{ItemTypeFactory, QName, SaxonApiException, SaxonApiUncheckedException, XPathExecutable, XdmAtomicValue, XdmItem, XdmNode, XdmNodeKind, XdmValue}
import net.sf.saxon.trans.XPathException
import net.sf.saxon.value.{SequenceType, UntypedAtomicValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.DynamicVariable

object SaxonExpressionEvaluator {
  val DEFAULT_COLLECTION = "https://xmlcalabash.com/default-collection"
  protected val _dynContext = new DynamicVariable[DynamicContext](null)
}

class SaxonExpressionEvaluator(xmlCalabash: XMLCalabashConfig) extends ExpressionEvaluator {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def withContext[T](context: DynamicContext)(thunk: => T): T = SaxonExpressionEvaluator._dynContext.withValue(context)(thunk)
  def dynContext: Option[DynamicContext] = Option(SaxonExpressionEvaluator._dynContext.value)

  override def newInstance(): SaxonExpressionEvaluator = {
    new SaxonExpressionEvaluator(xmlCalabash)
  }

  override def singletonValue(xpath: Any, context: List[Message], bindings: Map[String, Message], options: Option[Any]): XPathItemMessage = {
    val xdmval = value(xpath, context, bindings, options).item.asInstanceOf[XdmValue]

    if (xdmval.size() == 1) {
      new XPathItemMessage(xdmval, XProcMetadata.XML, xpath.asInstanceOf[XProcExpression].context)
    } else {
      xpath match {
        case xpath: XProcXPathExpression =>
          throw XProcException.xiSeqNotSupported(xpath.context.location, xpath)
        case xpath: XProcVtExpression =>
          val viter = xdmval.iterator()
          var s = ""
          var pos = 0
          while (viter.hasNext) {
            if (pos > 0) {
              s += " "
            }
            s += viter.next().getStringValue
          }
          new XPathItemMessage(xdmval, XProcMetadata.XML, xpath.context)
        // We should never fall off the end of this match because we got past the value() call above
      }
    }
  }

  override def value(xpath: Any, context: List[Message], bindings: Map[String, Message], options: Option[Any]): XPathItemMessage = {
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
      case xpath: XProcVtExpression =>
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
            case xitem: XdmItem =>
              checkDocument(newContext, xitem, value)
            case _ => Unit
          }
        case _ =>
          throw XProcException.xiInvalidMessage(newContext.location, value)
      }
    }

    val xdmvalue = withContext(newContext) { compute(xpath.asInstanceOf[XProcExpression], context, bindings, proxies.toMap, options) }

    val metadata = xdmvalue match {
      case node: XdmNode =>
        val baseURI = new XdmAtomicValue(node.getBaseURI)
        val pmap = mutable.Map.empty[QName,XdmItem]
        pmap.put(XProcConstants._base_uri, baseURI)
        new XProcMetadata(MediaType.XML, pmap.toMap)
      case _ => Unit
        // I'm suspicious that this isn't right in the general case
        XProcMetadata.XML
    }

    new XPathItemMessage(xdmvalue, metadata, xpath.asInstanceOf[XProcExpression].context)
  }

  override def booleanValue(xpath: Any, context: List[Message], bindings: Map[String, Message], options: Option[Any]): Boolean = {
    val xdmval = singletonValue(xpath, context, bindings, options).item.asInstanceOf[XdmValue]

    if (xdmval.size() == 1) {
      xdmval.itemAt(0) match {
        case atomic: XdmAtomicValue => atomic.getBooleanValue
        case _ => true
      }
    } else {
      // Is this right? Lists are always true?
      true
    }
  }

  override def precomputedValue(xpath: Any, value: Any, context: List[Message], bindings: Map[String, Message], options: Option[Any]): XPathItemMessage = {
    val config = xmlCalabash.processor.getUnderlyingConfiguration

    var xdmval = value match {
      case xdmval: XdmValue => xdmval
      case xpvv: XProcVarValue => xpvv.value
      case _ => throw new RuntimeException("Unexpected value type passed to precomputedValue") // FIXME:
    }

    val as = xpath match {
      case xxe: XProcXPathExpression => xxe.as
      case _ => None
    }

    val expr = xpath.asInstanceOf[XProcExpression]

    if (as.isDefined) {
      var matches = as.get.matches(xdmval.getUnderlyingValue, config.getTypeHierarchy)

      xdmval.getUnderlyingValue match {
        // Special case for untyped atomic values
        case ua: UntypedAtomicValue =>
          if (!matches) {
            // FIXME: This is a hack
            val castExpr = "\"" + ua.getStringValue.replace("\"", "\"\"") + "\" cast as " + as.get
            val cxpath = new XProcXPathExpression(expr.context, castExpr, as)
            try {
              val casted = singletonValue(cxpath, List(), Map(), None)
              xdmval = casted.item
              matches = true
            } catch {
              case _:  Throwable => Unit
            }
          }
        case _ =>
          Unit
      }

      if (!matches) {
        throw XProcException.dynamicError(36, List(xdmval, as.get), expr.context.location)
      }
    }

    new XPathItemMessage(xdmval, XProcMetadata.XML, expr.context)
  }

  def compute(xpath: XProcExpression, context: List[Message], bindings: Map[String, Message],
              proxies: Map[Any,XdmItem], options: Option[Any]): XdmValue = {
    val patchBindings = mutable.HashMap.empty[QName, XdmValue]

    for ((str, value) <- bindings) {
      value match {
        case item: ItemMessage =>
          item.item match {
            case xitem: XdmValue =>
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

    xpath match {
      case avtexpr: XProcVtExpression =>
        var xdmval: XdmValue = null
        var evalAvt = false
        for (part <- avtexpr.avt) {
          if (evalAvt) {
            val epart = computeValue(part, None, context, avtexpr.context, patchBindings.toMap, proxies, avtexpr.extensionFunctionsAllowed, options)
            if (xdmval == null) {
              xdmval = epart
            } else {
              xdmval = xdmval.append(epart)
            }
          } else {
            if (part != "") {
              if (xdmval == null) {
                xdmval = new XdmAtomicValue(part)
              } else {
                xdmval = xdmval.append(new XdmAtomicValue(part))
              }
            }
          }
          evalAvt = !evalAvt
        }

        if (avtexpr.stringResult) {
          val viter = xdmval.iterator()
          var s = ""
          var pos = 0
          while (viter.hasNext) {
            if (pos > 0) {
              s += " "
            }
            s += viter.next().getStringValue
            pos += 1
          }

          val typeFactory = new ItemTypeFactory(xmlCalabash.processor)
          val untypedAtomicType = typeFactory.getAtomicType(XProcConstants.xs_untypedAtomic)
          xdmval = new XdmAtomicValue(s, untypedAtomicType)
        }

        xdmval
      case xpathexpr: XProcXPathExpression =>
        computeValue(xpathexpr.expr, xpathexpr.as, context, xpathexpr.context, patchBindings.toMap, proxies, xpathexpr.extensionFunctionsAllowed, options)
      case _ =>
        throw XProcException.xiUnexpectedExprType(xpath.context.location, xpath)
    }
  }

  private def computeValue(xpath: String,
                           as: Option[SequenceType],
                           contextItem: List[Message],
                           exprContext: ExpressionContext,
                           bindings: Map[QName,XdmValue],
                           proxies: Map[Any, XdmItem],
                           extensionsOk: Boolean,
                           options: Option[Any]): XdmValue = {
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
                throw new XProcException(sae.getErrorCode,sae.getMessage)
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
        selector.setVariable(varname, varvalue)
      }

      if (contextItem.size == 1) {
        contextItem.head match {
          case item: ItemMessage =>
            item.item match {
              case node: XdmNode =>
                selector.setContextItem(proxies(node))
              case _ =>
                selector.setContextItem(proxies(item.item))
            }
        }
      }

      val value = try {
        selector.evaluate()
      } catch {
        case sae: SaxonApiException =>
          if (sae.getMessage.contains("context item") && sae.getMessage.contains("absent")) {
            if (contextItem.size > 1) {
              throw XProcException.xdContextItemSequence(xpath, sae.getMessage, exprContext.location)
            } else {
              throw XProcException.xdContextItemAbsent(xpath, sae.getMessage, exprContext.location)
            }
          } else {
            throw sae
          }
        case ex: Exception =>
          throw ex
      }

      if (as.isDefined) {
        val matches = as.get.matches(value.getUnderlyingValue, config.getTypeHierarchy)
        if (!matches) {
          throw XProcException.dynamicError(36, List(value, as.get), exprContext.location)
        }
      }

      value
    } catch {
      case saue: SaxonApiUncheckedException =>
        saue.getCause match {
          case xpe: XPathException =>
            val code = new QName(xpe.getErrorCodeNamespace, xpe.getErrorCodeLocalPart)
            throw new StepException(code, xpe.getMessage, xpe, exprContext.location)
          case _ => throw saue
        }
      case ex: Exception =>
        throw ex
    }
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
      case item: XdmItem =>
        dynContext.addItem(item.getUnderlyingValue, msg)
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
                case atom: XdmAtomicValue => builder.addValues(value)
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
