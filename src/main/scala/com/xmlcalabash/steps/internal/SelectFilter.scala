package com.xmlcalabash.steps.internal

import com.jafpl.graph.Location
import com.jafpl.messages.Message
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{AnyItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.runtime.params.SelectFilterParams
import com.xmlcalabash.runtime.{BinaryNode, ImplParams, StaticContext, XMLCalabashRuntime, XProcDataConsumer, XProcMetadata, XProcXPathExpression, XmlPortSpecification, XmlStep}
import com.xmlcalabash.util.{MediaType, XProcVarValue}
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode, XdmNodeKind, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Performs XPath selections on the document(s) that flow through it
  *
  * This is an internal step, it is not intended to be instantiated by pipeline authors.
  */
class SelectFilter() extends XmlStep {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[XProcDataConsumer] = None
  protected var config: XMLCalabashRuntime = _
  protected val bindings = mutable.HashMap.empty[String,Message]
  protected var allowedTypes = List.empty[MediaType]
  protected var portName: String = _
  protected var sequence = false
  private val nodeMeta = mutable.HashMap.empty[Any, XProcMetadata]
  private val nodes = ListBuffer.empty[Any]
  private var _location = Option.empty[Location]
  private var select: String = _
  private var selectContext: StaticContext = _

  def location: Option[Location] = _location

  // ==========================================================================

  override def setLocation(location: Location): Unit = {
    _location = Some(location)
  }

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(variable: QName, value: XdmValue, context: StaticContext): Unit = {
    value match {
      case node: XdmNode =>
        bindings.put(variable.getClarkName, new XdmNodeItemMessage(node, XProcMetadata.xml(node), context))
      case item: XdmItem =>
        bindings.put(variable.getClarkName, new XdmValueItemMessage(item, XProcMetadata.JSON, context))
    }
  }

  override def setConsumer(consumer: XProcDataConsumer): Unit = {
    this.consumer = Some(consumer)
  }

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    nodes += item
    nodeMeta.put(item,metadata)
    /*
    item match {
      case node: XdmValue =>
        nodes += node
        nodeMeta.put(node, metadata)
      case _ =>
        throw new RuntimeException("Cannot filter non-XML inputs")
    }

     */
  }

  override def configure(config: XMLCalabashConfig, params: Option[ImplParams]): Unit = {
    if (params.isEmpty) {
      throw XProcException.xiWrongImplParams()
    } else {
      params.get match {
        case cp: SelectFilterParams =>
          select = cp.select
          selectContext = cp.context
        case _ => throw XProcException.xiWrongImplParams()
      }
    }
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    config match {
      case xmlCalabash: XMLCalabashRuntime => this.config = xmlCalabash
      case _ => throw XProcException.xiNotXMLCalabash()
    }
  }

  override def run(context: StaticContext): Unit = {
    for ((name, message) <- selectContext.statics) {
      if (!bindings.contains(name)) {
        bindings.put(name, message)
      }
    }

    for (node <- nodes) {
      val metadata = nodeMeta(node)
      val expr = new XProcXPathExpression(selectContext, select, None, None, None)
      val msg = node match {
        case value: XdmNode => new XdmNodeItemMessage(value, metadata, selectContext)
        case value: XdmValue => new XdmValueItemMessage(value, metadata, selectContext)
        case value: BinaryNode =>
          val tree = new SaxonTreeBuilder(config)
          tree.startDocument(metadata.baseURI)
          tree.endDocument()
          new AnyItemMessage(tree.result, value, metadata, selectContext)
        case _ => throw new RuntimeException("fred")
      }
      val exprEval = config.expressionEvaluator.newInstance()
      val result = exprEval.value(expr, List(msg), bindings.toMap, None)
      val xdmvalue = result.item
      val iter = xdmvalue.iterator()
      while (iter.hasNext) {
        val item = iter.next()
        item match {
          case node: XdmNode =>
            if (node.getNodeKind == XdmNodeKind.ATTRIBUTE) {
              throw XProcException.xdInvalidSelection(select, "attribute", location)
            }
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(node.getBaseURI)
            tree.addSubtree(node)
            tree.endDocument()
            consumer.get.receive("result", tree.result, XProcMetadata.xml(node))
          case value: XdmItem =>
            consumer.get.receive("result", value, XProcMetadata.JSON)
          case _ =>
            throw new RuntimeException(s"Didn't expect that $item")
        }
      }
    }
  }

  override def reset(): Unit = {
    // nop
  }

  override def abort(): Unit = {
    // nop
  }

  override def stop(): Unit = {
    // nop
  }

  override def toString: String = {
    val defStr = super.toString
    if (defStr.startsWith("com.xmlcalabash.steps")) {
      val objstr = ".*\\.([^\\.]+)@[0-9a-f]+$".r
      defStr match {
        case objstr(name) => name
        case _ => defStr
      }
    } else {
      defStr
    }
  }
}
