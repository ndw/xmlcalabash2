package com.xmlcalabash.runtime

import java.io.{IOException, InputStream}

import com.jafpl.graph.Location
import com.jafpl.messages.{BindingMessage, ExceptionMessage, Message}
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, PortCardinality, Step}
import com.xmlcalabash.exceptions.{StepException, XProcException}
import com.xmlcalabash.messages.{AnyItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.s9api.{QName, XdmArray, XdmAtomicValue, XdmMap, XdmNode, XdmNodeKind, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class StepProxy(config: XMLCalabashRuntime, stepType: QName, step: StepWrapper, params: Option[ImplParams], context: StaticContext) extends Step with XProcDataConsumer {
  private val typeUtils = new TypeUtils(config)
  private var location = Option.empty[Location]
  private var _id: String = _
  private val openStreams = ListBuffer.empty[InputStream]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[DataConsumer] = None
  protected val bindings = mutable.HashSet.empty[QName]
  protected val bindingsMap = mutable.HashMap.empty[String, Message]
  protected var dynamicContext = new DynamicContext()
  protected var received = mutable.HashSet.empty[String]

  protected var defaultSelect = mutable.HashMap.empty[String, XProcExpression]

  def nodeId: String = _id
  def nodeId_=(id: String): Unit = {
    if (_id == null) {
      _id = id
    } else {
      throw XProcException.xiRedefId(id, location)
    }
  }

  def setDefaultSelect(port: String, select: XProcExpression): Unit = {
    defaultSelect.put(port, select)
  }

  // =============================================================================================

  override def toString: String = {
    val node = config.node(nodeId)
    if (node.isDefined) {
      node.get.toString
    } else {
      "proxy:" + step.toString
    }
  }

  override def inputSpec: XmlPortSpecification = {
    step match {
      case xstep: XmlStep => xstep.inputSpec
      case _ =>
        val portMap = mutable.HashMap.empty[String,PortCardinality]
        val typeMap = mutable.HashMap.empty[String,List[String]]
        for (key <- step.inputSpec.ports) {
          portMap.put(key, step.inputSpec.cardinality(key).getOrElse(PortCardinality.ZERO_OR_MORE))
          typeMap.put(key, List("application/octet-stream"))
        }
        new XmlPortSpecification(portMap.toMap, typeMap.toMap)
    }
  }

  override def outputSpec: XmlPortSpecification = {
    step match {
      case xstep: XmlStep => xstep.outputSpec
      case _ =>
        val portMap = mutable.HashMap.empty[String,PortCardinality]
        val typeMap = mutable.HashMap.empty[String,List[String]]
        for (key <- step.outputSpec.ports) {
          portMap.put(key, step.outputSpec.cardinality(key).getOrElse(PortCardinality.ZERO_OR_MORE))
          typeMap.put(key, List("application/octet-stream"))
        }
        new XmlPortSpecification(portMap.toMap, typeMap.toMap)
    }
  }
  override def bindingSpec: BindingSpecification = step.bindingSpec
  override def setConsumer(consumer: DataConsumer): Unit = {
    this.consumer = Some(consumer)
    step.setConsumer(this)
  }
  override def setLocation(location: Location): Unit = {
    this.location = Some(location)
    step.setLocation(location)
  }
  override def id: String = _id
  override def receiveBinding(bindmsg: BindingMessage): Unit = {
    val qname = if (bindmsg.name.startsWith("{")) {
      val clarkName = "\\{(.*)\\}(.*)".r
      val qname = bindmsg.name match {
        case clarkName(uri,name) => new QName(uri,name)
        case _ => throw XProcException.xiInvalidClarkName(location, bindmsg.name)
      }
      qname
    } else {
      new QName("", bindmsg.name)
    }

    bindings += qname
    bindingsMap.put(qname.getClarkName, bindmsg.message)

    val stepsig = step.signature
    if (stepsig.options.contains(qname)) {
      val optsig  = stepsig.option(qname, location.get)
      val opttype: Option[QName] = if (optsig.declaredType.isDefined) {
        Some(new QName(XProcConstants.ns_xs, optsig.declaredType.get))
      } else {
        None
      }
      val occurrence = optsig.occurrence

      bindmsg.message match {
        case item: XdmValueItemMessage =>
          item.item match {
            case atomic: XdmAtomicValue =>
              if (false && occurrence.isDefined) {
                val seq = typeUtils.castSequenceAs(atomic, opttype, occurrence.get, item.context)
                step.receiveBinding(qname, seq, item.context)
              } else {
                val value = typeUtils.castAtomicAs(atomic, opttype, item.context)
                step.receiveBinding(qname, value, item.context)
              }
            case _ => Unit
              step.receiveBinding(qname, item.item, item.context)
          }
        case _ =>
          throw XProcException.xiInvalidMessage(location, bindmsg.message)
      }
    }
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    config match {
      case saxon: XMLCalabashRuntime => Unit
      case _ => throw XProcException.xiNotXMLCalabash()
    }
    if (this.config != config) {
      throw XProcException.xiDifferentXMLCalabash()
    }
    step.initialize(config, params)
  }

  override def run(): Unit = {
    for (port <- defaultSelect.keySet) {
      if (!received.contains(port)) {
        // If the input has a select, this is the context for that expression
        val expr = config.expressionEvaluator.newInstance()
        val selectExpr = defaultSelect(port)
        val selected = expr.value(selectExpr, List(), bindingsMap.toMap, None)
        val iter = selected.item.iterator()
        while (iter.hasNext) {
          val item = iter.next()
          item match {
            case node: XdmNode =>
              if (node.getNodeKind == XdmNodeKind.ATTRIBUTE) {
                throw XProcException.xdInvalidSelection(selectExpr.toString, "an attribute", location)
              }
              // There's no message, so??? dynamicContext.addDocument(node, message)
            case _ => Unit
          }
          step.receive(port, item, selected.metadata)
        }
      }
    }


    for (qname <- step.signature.options) {
      if (!bindings.contains(qname)) {
        val optsig  = step.signature.option(qname, location.get)
        val opttype: Option[QName] = if (optsig.declaredType.isDefined) {
          Some(new QName(XProcConstants.ns_xs, optsig.declaredType.get))
        } else {
          None
        }
        if (optsig.defaultValue.isDefined) {
          val value = typeUtils.castAtomicAs(new XdmAtomicValue(optsig.defaultValue.get), opttype, ExpressionContext.NONE)
          step.receiveBinding(qname, value, ExpressionContext.NONE)
        }
      }
    }

    try {
      DynamicContext.withContext(dynamicContext) { step.run(context) }
    } finally {
      var thrown = Option.empty[Exception]
      for (stream <- openStreams) {
        try {
          stream.close()
        } catch {
          case ex: IOException => Unit
          case ex: Exception =>
            thrown = Some(ex)
        }
      }
      if (thrown.isDefined) {
        throw thrown.get
      }
    }
  }

  override def reset(): Unit = {
    step.reset()
    bindings.clear()
    bindingsMap.clear()
    received.clear()
  }

  override def abort(): Unit = {
    step.abort()
  }

  override def stop(): Unit = {
    step.stop()
  }

  override def receive(port: String, message: Message): Unit = {
    received += port

    message match {
      case msg: ExceptionMessage =>
        msg.item match {
          case ex: StepException =>
            if (ex.errors.isDefined) {
              step.receive(port, ex.errors.get, XProcMetadata.XML)
            } else {
              step.receive(port, msg.item, XProcMetadata.EXCEPTION)
            }
          case _ =>
            step.receive(port, msg.item, XProcMetadata.EXCEPTION)
        }

      case msg: XdmValueItemMessage =>
        if (defaultSelect.contains(port)) {
          // If the input has a select, this is the context for that expression
          val expr = config.expressionEvaluator.newInstance()
          val selectExpr = defaultSelect(port)
          val selected = expr.value(selectExpr, List(msg), bindingsMap.toMap, None)
          val iter = selected.item.iterator()
          while (iter.hasNext) {
            val item = iter.next()
            item match {
              case node: XdmNode =>
                if (node.getNodeKind == XdmNodeKind.ATTRIBUTE) {
                  throw XProcException.xdInvalidSelection(selectExpr.toString, "an attribute", location)
                }
                dynamicContext.addDocument(node, message)
              case _ => Unit
            }
            step.receive(port, item, selected.metadata)
          }
        } else {
          step.receive(port, msg.item, msg.metadata)
        }
      case msg: AnyItemMessage =>
        step.receive(port, msg.shadow, msg.metadata)
      case _ => throw XProcException.xiInvalidMessage(location, message)
    }
  }

  // =======================================================================================

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case value: XdmNode =>
        consumer.get.receive(port, new XdmNodeItemMessage(value, metadata))
      case value: XdmValue =>
        value match {
          case _: XdmMap =>
            val msg = new XdmValueItemMessage(value, new XProcMetadata(MediaType.JSON, metadata))
            consumer.get.receive(port, msg)
          case _: XdmArray =>
            val msg = new XdmValueItemMessage(value, new XProcMetadata(MediaType.JSON, metadata))
            consumer.get.receive(port, msg)
          case _ =>
            // FIXME: Really?
            val msg = new XdmValueItemMessage(value, new XProcMetadata(MediaType.JSON, metadata))
            consumer.get.receive(port, msg)
        }
      case _ =>
        val tree = new SaxonTreeBuilder(config)
        tree.startDocument(metadata.baseURI)
        tree.endDocument()
        consumer.get.receive(port, new AnyItemMessage(tree.result, item, metadata))
    }
  }
}
