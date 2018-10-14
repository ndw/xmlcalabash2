package com.xmlcalabash.runtime

import com.jafpl.graph.Location
import com.jafpl.messages.{BindingMessage, ExceptionMessage, ItemMessage, Message}
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, PortCardinality, PortSpecification, Step}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{StepException, XProcException}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.util.{TypeUtils, XProcVarValue}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmNode, XdmNodeKind}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class StepProxy(config: XMLCalabash, stepType: QName, step: XmlStep, params: Option[ImplParams], context: StaticContext) extends Step with XProcDataConsumer {
  private val typeUtils = new TypeUtils(config)
  private var location = Option.empty[Location]
  private var _id: String = _
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[DataConsumer] = None
  protected val bindings = mutable.HashSet.empty[QName]
  protected val bindingsMap = mutable.HashMap.empty[String, Message]
  protected var dynamicContext = new DynamicContext()

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

    val stepsig = config.signatures.step(stepType)
    if (stepsig.options.contains(qname)) {
      val optsig  = stepsig.option(qname, location.get)
      val opttype: Option[QName] = if (optsig.declaredType.isDefined) {
        Some(new QName(XProcConstants.ns_xs, optsig.declaredType.get))
      } else {
        None
      }
      val occurrence = optsig.occurrence

      bindmsg.message match {
        case item: XPathItemMessage =>
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
        case item: ItemMessage =>
          item.item match {
            case atomic: XdmAtomicValue =>
              if (false && occurrence.isDefined) {
                val seq = typeUtils.castSequenceAs(atomic, opttype, occurrence.get, ExpressionContext.NONE)
                step.receiveBinding(qname, seq, ExpressionContext.NONE)
              } else {
                val value = typeUtils.castAtomicAs(atomic, opttype, ExpressionContext.NONE)
                step.receiveBinding(qname, value, ExpressionContext.NONE)
              }
            case opt: XProcVarValue =>
              step.receiveBinding(qname, opt.value, ExpressionContext.NONE)
            case _ => Unit
              step.receiveBinding(qname, item.item.asInstanceOf[XdmItem], ExpressionContext.NONE)
          }
        case _ =>
          throw XProcException.xiInvalidMessage(location, bindmsg.message)
      }
    }
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    config match {
      case saxon: XMLCalabash => Unit
      case _ => throw XProcException.xiNotXMLCalabash()
    }
    if (this.config != config) {
      throw XProcException.xiDifferentXMLCalabash()
    }
    step.initialize(config, params)
  }
  override def run(): Unit = {
    val stepsig = config.signatures.step(stepType)
    for (qname <- stepsig.options) {
      if (!bindings.contains(qname)) {
        val optsig  = stepsig.option(qname, location.get)
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

    DynamicContext.withContext(dynamicContext) { step.run(context) }
  }
  override def reset(): Unit = {
    step.reset()
    bindings.clear()
    bindingsMap.clear()
  }
  override def abort(): Unit = {
    step.abort()
  }
  override def stop(): Unit = {
    step.stop()
  }
  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ExceptionMessage =>
        item.item match {
          case ex: StepException =>
            if (ex.errors.isDefined) {
              step.receive(port, ex.errors.get, XProcMetadata.XML)
            } else {
              step.receive(port, item.item, XProcMetadata.EXCEPTION)
            }
          case _ =>
            step.receive(port, item.item, XProcMetadata.EXCEPTION)
        }

      case item: ItemMessage =>
        item.metadata match {
          case xmlmeta: XProcMetadata =>
            if (defaultSelect.contains(port)) {
              val expr = config.expressionEvaluator.newInstance()
              val selectExpr = defaultSelect(port)
              val selected = expr.value(selectExpr, List(item), bindingsMap.toMap, None)
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
              step.receive(port, item.item, xmlmeta)
            }
          case _ => throw XProcException.xiInvalidMetadata(location, item.metadata)
        }
      case _ => throw XProcException.xiInvalidMessage(location, message)
    }
  }

  // =======================================================================================

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    consumer.get.receive(port, new ItemMessage(item, metadata))
  }
}
