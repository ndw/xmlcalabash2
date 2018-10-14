package com.xmlcalabash.steps.internal

import com.jafpl.graph.Location
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{ExpressionContext, ImplParams, StaticContext, XProcDataConsumer, XProcMetadata, XmlPortSpecification, XmlStep}
import com.xmlcalabash.util.{MediaType, XProcVarValue}
import net.sf.saxon.s9api.{QName, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class ContentTypeParams(val port: String, val contentTypes: List[MediaType], val sequence: Boolean) extends ImplParams {
}

/**
  * Checks the content type and cardinality of documents that flow through it.
  *
  * This is an internal step, it is not intended to be instantiated by pipeline authors.
  * It's also slightly misnamed as it checks both the content type and cardinality of
  * the documents that flow through it.
  *
  */
class ContentTypeChecker() extends XmlStep {
  private var _location = Option.empty[Location]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[XProcDataConsumer] = None
  protected var config: XMLCalabash = _
  protected val bindings = mutable.HashMap.empty[QName,XProcVarValue]
  protected var allowedTypes: List[MediaType] = _
  protected var portName: String = _
  protected var sequence = false
  protected var documentCount = 0

  def location: Option[Location] = _location

  // ==========================================================================

  override def setLocation(location: Location): Unit = {
    _location = Some(location)
  }

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(variable: QName, value: XdmValue, context: ExpressionContext): Unit = {
    bindings.put(variable, new XProcVarValue(value, context))
  }

  override def setConsumer(consumer: XProcDataConsumer): Unit = {
    this.consumer = Some(consumer)
  }

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (allowedTypes.nonEmpty) {
      var allowed = false
      for (ctype <- allowedTypes) {
        allowed = allowed || metadata.contentType.matches(ctype)
      }

      if (!allowed) {
        throw XProcException.xdBadMediaType(metadata.contentType, allowedTypes)
      }
    }
    consumer.get.receive("result", item, metadata)
  }

  override def initialize(config: RuntimeConfiguration, params: Option[ImplParams]): Unit = {
    config match {
      case xmlCalabash: XMLCalabash => this.config = xmlCalabash
      case _ => throw XProcException.xiNotXMLCalabash()
    }

    if (params.isEmpty) {
      throw XProcException.xiWrongImplParams()
    } else {
      params.get match {
        case cp: ContentTypeParams =>
          allowedTypes = cp.contentTypes
          portName = cp.port
          sequence = cp.sequence
        case _ => throw XProcException.xiWrongImplParams()
      }
    }
  }

  override def run(context: StaticContext): Unit = {
    // nop; checking is done as the inputs arrive
  }

  override def reset(): Unit = {
    documentCount = 0
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
