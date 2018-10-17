package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.graph.{Graph, Location}
import com.jafpl.messages.Message
import com.jafpl.runtime.{GraphRuntime, RuntimeConfiguration}
import com.jafpl.steps.DataConsumer
import com.jafpl.util.{ErrorListener, TraceEventManager}
import com.xmlcalabash.config.{DocumentManager, Signatures, XMLCalabashConfig, XMLCalabashDebugOptions}
import com.xmlcalabash.exceptions.{ConfigurationException, ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.ExpressionParser
import com.xmlcalabash.model.xml.{Artifact, DeclareStep}
import com.xmlcalabash.util.{SerializationOptions, XProcVarValue}
import net.sf.saxon.s9api.{Processor, QName, XdmValue}

import scala.collection.mutable

class XMLCalabashRuntime protected[xmlcalabash] (val config: XMLCalabashConfig,
                                                 private val debug: XMLCalabashDebugOptions) extends RuntimeConfiguration {
  private var _traceEventManager = config.traceEventManager
  private var _errorListener = config.errorListener
  private var _documentManager = config.documentManager
  private var _watchdogTimeout = config.watchdogTimeout
  private val _staticOptionBindings = mutable.HashMap.empty[QName, XdmValue]
  private val inputSet = mutable.HashSet.empty[String]
  private val outputSet = mutable.HashSet.empty[String]
  private val bindingsMap = mutable.HashMap.empty[String,Message]
  private val idMap = mutable.HashMap.empty[String,Artifact]
  private var ran = false
  private var _signatures: Signatures = _
  private var decl: DeclareStep = _
  private var graph: Graph = _
  private var runtime: GraphRuntime = _

  protected[xmlcalabash] def init(decl: DeclareStep): Unit = {
    this.decl = decl

    debug.dumpXml(decl)
    graph = decl.pipelineGraph()
    debug.dumpOpenGraph(graph)
    graph.close()
    runtime = new GraphRuntime(graph, this)
    runtime.traceEventManager = _traceEventManager
  }

  // ===================================================================================

  def inputs: List[String] = decl.inputPorts
  def outputs: List[String] = decl.outputPorts

  def input(port: String, item: XdmValue, metadata: XProcMetadata): Unit = {
    if (runtime.inputs.contains(port)) {
      inputSet += port
      runtime.inputs(port).send(new XPathItemMessage(item, metadata, ExpressionContext.NONE))
    }
  }

  def input(port: String, message: Message): Unit = {
    if (runtime.inputs.contains(port)) {
      inputSet += port
      runtime.inputs(port).send(message)
    }
  }

  def output(port: String, consumer: DataConsumer): Unit = {
    runtime.outputs(port).setConsumer(consumer)
  }

  def serializationOptions(port: String): SerializationOptions = {
    decl.output(port).get.serialization
  }

  def option(name: QName, value: XProcVarValue): Unit = {
    if (decl.bindings.contains(name)) {
      config.trace(s"Binding option $name to '$value'", "ExternalBindings")
      val msg = new XPathItemMessage(value.value, XProcMetadata.XML, value.context)
      runtime.setOption(name.getClarkName, value)
      bindingsMap.put(name.getClarkName, msg)
    }
  }

  def run(): Unit = {
    if (ran) {
      throw new RuntimeException("You must call reset() before running a pipeline a second time.")
    }

    ran = true

    for (input <- decl.inputs) {
      if (!inputSet.contains(input.port.get) && input.defaultInputs.isEmpty && !input.sequence) {
        throw XProcException.xsMissingRequiredInput(input.port.get, input.location)
      }
    }

    for (bind <- decl.bindings) {
      val jcbind = bind.getClarkName
      if (!bindingsMap.contains(jcbind)) {
        config.trace(s"No binding provided for option $bind; using default", "ExternalBindings")
        val bdecl = decl.bindingDeclaration(bind)
        if (bdecl.isDefined) {
          if (bdecl.get.select.isDefined) {
            val context = new ExpressionContext(None, Map.empty[String,String], None) // FIXME: what about namespaces!?
            val expr = new XProcXPathExpression(context, bdecl.get.select.get)
            val msg = config.expressionEvaluator.value(expr, List(), bindingsMap.toMap, None)
            val eval = msg.asInstanceOf[XPathItemMessage].item
            runtime.setOption(jcbind, new XProcVarValue(eval, context))
            bindingsMap.put(jcbind, msg)
          } else {
            if (bdecl.get.required) {
              throw XProcException.xsMissingRequiredOption(bind, decl.location)
            } else {
              val context = new ExpressionContext(None, Map.empty[String,String], None) // FIXME: what about namespaces!?
              val expr = new XProcXPathExpression(context, "()")
              val msg = config.expressionEvaluator.value(expr, List(), bindingsMap.toMap, None)
              val eval = msg.asInstanceOf[XPathItemMessage].item
              runtime.setOption(jcbind, new XProcVarValue(eval, context))
              bindingsMap.put(jcbind, msg)
            }
          }
        } else {
          println("No decl for " + bind + " ???")
        }
      }
    }

    try {
      runtime.run()
    } catch {
      case ex: Exception =>
        runtime.stop()
        throw ex
    }
  }

  def reset(): Unit = {
    _staticOptionBindings.clear()
    inputSet.clear()
    outputSet.clear()
    bindingsMap.clear()

    graph = decl.pipelineGraph()
    graph.close()
    runtime = new GraphRuntime(graph, this)
    runtime.traceEventManager = _traceEventManager
    ran = false
  }

  def stop(): Unit = {
    if (!ran) {
      runtime.stop()
    }
  }

  // ===================================================================================

  def productName: String = config.productName
  def productVersion: String = config.productVersion
  def jafplVersion: String = config.jafplVersion
  def saxonVersion: String = config.saxonVersion
  def productConfig: String = config.productConfig
  def vendor: String = config.vendor
  def vendorURI: String = config.vendorURI
  def xprocVersion: String = config.xprocVersion
  def xpathVersion: String = config.xpathVersion
  def psviSupported: Boolean = config.psviSupported
  def processor: Processor = config.processor
  def staticBaseURI: URI = config.staticBaseURI

  def watchdogTimeout: Long = _watchdogTimeout
  def watchdogTimeout_=(timeout: Long): Unit = {
    _watchdogTimeout = timeout
  }

  def documentManager: DocumentManager = _documentManager
  def documentManager_=(manager: DocumentManager): Unit = {
    _documentManager = manager
  }

  def errorListener: ErrorListener = _errorListener
  def errorListener_=(listener: ErrorListener): Unit = {
    _errorListener = listener
  }

  def traceEventManager: TraceEventManager = _traceEventManager
  def traceEventManager_=(manager: TraceEventManager): Unit = {
    _traceEventManager = manager
  }
  override def traceEnabled(trace: String): Boolean = _traceEventManager.traceEnabled(trace)

  def staticOptionValue(option: QName): Option[XdmValue] = _staticOptionBindings.get(option)
  def setStaticOptionValue(option: QName, value: XdmValue): Unit = {
    _staticOptionBindings.put(option, value)
  }

  override def expressionEvaluator: SaxonExpressionEvaluator = config.expressionEvaluator
  def expressionParser: ExpressionParser = config.expressionParser

  // ====================================================================================

  def addNode(id: String, artifact: Artifact): Unit = {
    idMap.put(id, artifact)
  }

  def node(id: String): Option[Artifact] = idMap.get(id)


  def signatures: Signatures = {
    if (_signatures == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "signatures")
    }
    _signatures
  }

  def signatures_=(signatures: Signatures): Unit = {
    _signatures = signatures
  }

  // ==============================================================================================

  def stepImplementation(stepType: QName, location: Location): StepWrapper = {
    stepImplementation(stepType, location, None)
  }

  def stepImplementation(stepType: QName, location: Location, implParams: Option[ImplParams]): StepWrapper = {
    if (!_signatures.stepTypes.contains(stepType)) {
      throw new ModelException(ExceptionCode.NOTYPE, stepType.toString, location)
    }

    val sig = _signatures.step(stepType)
    val implClass = sig.implementation
    if (implClass.isEmpty) {
      throw new ModelException(ExceptionCode.NOIMPL, stepType.toString, location)
    }

    val klass = Class.forName(implClass.head).newInstance()
    klass match {
      case step: XmlStep =>
        new StepWrapper(step, sig)
      case _ =>
        throw new ModelException(ExceptionCode.IMPLNOTSTEP, stepType.toString, location)
    }
  }
}
