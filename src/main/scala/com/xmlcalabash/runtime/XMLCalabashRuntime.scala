package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.graph.Graph
import com.jafpl.messages.{JoinGateMessage, Message}
import com.jafpl.runtime.{GraphRuntime, RuntimeConfiguration}
import com.jafpl.steps.DataConsumer
import com.jafpl.util.{ErrorListener, TraceEventManager}
import com.xmlcalabash.config.{DocumentManager, Signatures, XMLCalabashConfig, XMLCalabashDebugOptions, XProcConfigurer}
import com.xmlcalabash.exceptions.{ConfigurationException, ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{ExpressionParser, XProcConstants}
import com.xmlcalabash.model.xml.{Artifact, DeclareStep}
import com.xmlcalabash.util.{MediaType, XProcVarValue}
import javax.xml.transform.URIResolver
import net.sf.saxon.lib.{ModuleURIResolver, UnparsedTextURIResolver}
import net.sf.saxon.s9api.{Processor, QName, XdmAtomicValue, XdmValue}
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.EntityResolver

import scala.collection.mutable

class XMLCalabashRuntime protected[xmlcalabash] (val config: XMLCalabashConfig) extends RuntimeConfiguration {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected[runtime] val joinGateMarker = new XdmAtomicValue(new QName(XProcConstants.ns_cx, "JOIN-GATE-MARKER"))

  private var _traceEventManager = config.traceEventManager
  private var _errorListener = config.errorListener
  private var _documentManager = config.documentManager
  private var _entityResolver = config.entityResolver
  private var _uriResolver = config.uriResolver
  private var _moduleURIResolver = config.moduleURIResolver
  private var _unparsedTextURIResolver = config.unparsedTextURIResolver
  private var _watchdogTimeout = config.watchdogTimeout
  private var _episode = config.computeEpisode
  private var _defaultSerializationOptions: Map[String,Map[QName,String]] = Map.empty[String,Map[QName,String]]
  private var _trim_inline_whitespace = config.trimInlineWhitespace
  private val inputSet = mutable.HashSet.empty[String]
  private val outputSet = mutable.HashSet.empty[String]
  private val bindingsMap = mutable.HashMap.empty[String,XdmValue]
  private val idMap = mutable.HashMap.empty[String,Artifact]
  private var ran = false
  private var _signatures: Signatures = _
  private var decl: DeclareStep = _
  private var graph: Graph = _
  private var runtime: GraphRuntime = _

  protected[xmlcalabash] def setDeclaration(decl: DeclareStep): Unit = {
    this.decl = decl
  }

  protected[xmlcalabash] def init(): Unit = {
    config.debugOptions.dumpXml(decl)
    graph = decl.pipelineGraph()
    config.debugOptions.dumpOpenGraph(graph, decl)
    graph.close()
    config.debugOptions.dumpGraph(graph, decl)
    runtime = new GraphRuntime(graph, this)
    runtime.traceEventManager = _traceEventManager
  }

  // ===================================================================================

  def inputs: List[String] = decl.inputPorts
  def outputs: List[String] = decl.outputPorts

  protected[runtime] def inputMessage(port: String, msg: Message): Unit = {
    runtime.inputs(port).send(msg)
  }

  def input(port: String, item: XdmValue, metadata: XProcMetadata): Unit = {
    if (runtime.inputs.contains(port)) {
      inputSet += port
      runtime.inputs(port).send(new XdmValueItemMessage(item, metadata, ExpressionContext.NONE))
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

  def serializationOptions(port: String): Map[QName,String] = {
    decl.output(port).get.serialization
  }

  def option(name: QName, value: String): Unit = {
    option(name, new XProcVarValue(new XdmAtomicValue(value), ExpressionContext.NONE))
  }

  def option(name: QName, value: Integer): Unit = {
    option(name, new XProcVarValue(new XdmAtomicValue(value), ExpressionContext.NONE))
  }

  def option(name: QName, value: Float): Unit = {
    option(name, new XProcVarValue(new XdmAtomicValue(value), ExpressionContext.NONE))
  }

  def option(name: QName, value: URI): Unit = {
    option(name, new XProcVarValue(new XdmAtomicValue(value), ExpressionContext.NONE))
  }

  def option(name: QName, value: XProcVarValue): Unit = {
    if (decl.bindings.contains(name)) {
      config.trace(s"Binding option $name to '$value'", "ExternalBindings")
      //val msg = new XdmValueItemMessage(value.value, XProcMetadata.XML, value.context)
      //runtime.setOption(name.getClarkName, value)
      bindingsMap.put(name.getClarkName, value.value)
    }
  }

  protected[runtime] def runAsStep(): Unit = {
    if (ran) {
      throw new RuntimeException("You must call reset() before running a pipeline a second time.")
    }

    runCommon()
  }

  def run(): Unit = {
    if (ran) {
      throw new RuntimeException("You must call reset() before running a pipeline a second time.")
    }

    for (input <- decl.inputs) {
      if (!inputSet.contains(input.port.get)) {
        if (input.defaultInputs.isEmpty) {
          if (!input.sequence) {
            throw XProcException.xsMissingRequiredInput(input.port.get, input.location)
          }
        } else {
          runtime.inputs(input.port.get).send(new JoinGateMessage())
        }
      }

      if (!inputSet.contains(input.port.get) && input.defaultInputs.isEmpty && !input.sequence) {
        throw XProcException.xsMissingRequiredInput(input.port.get, input.location)
      }
    }

    runCommon()
  }

  private def runCommon(): Unit = {
    ran = true

    for (bind <- decl.bindings) {
      val jcbind = bind.getClarkName
      val bdecl = decl.bindingDeclaration(bind).get
      if (bindingsMap.contains(jcbind)) {
        bdecl.externalValue = bindingsMap(jcbind)
      } else {
        bdecl.externalValue = None
      }

      if (bindingsMap.contains(jcbind)) {
        runtime.setOption(jcbind, new XProcVarValue(bindingsMap(jcbind), new ExpressionContext(new StaticContext())))
      } else {
        if (bdecl.required) {
          throw XProcException.xsMissingRequiredOption(bind, decl.location)
        }
      }
    }

    decl.evaluateStaticBindings(runtime)
    decl.propagateStaticBindings()

    try {
      runtime.run()
    } finally {
      runtime.stop()
    }
  }

  def reset(): Unit = {
    //_staticOptionBindings.clear()
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
    runtime.stop()
  }

  // ===================================================================================

  def xprocConfigurer: XProcConfigurer = config.xprocConfigurer

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
  def episode: String = _episode

  def watchdogTimeout: Long = _watchdogTimeout
  def watchdogTimeout_=(timeout: Long): Unit = {
    _watchdogTimeout = timeout
  }

  // FIXME: Setters for these
  def entityResolver: EntityResolver = _entityResolver
  def uriResolver: URIResolver = _uriResolver
  def moduleURIResolver: ModuleURIResolver = _moduleURIResolver
  def unparsedTextURIResolver: UnparsedTextURIResolver = _unparsedTextURIResolver

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

  override def expressionEvaluator: SaxonExpressionEvaluator = config.expressionEvaluator
  def expressionParser: ExpressionParser = config.expressionParser

  // Combine the specified bindings with any external, global bindings
  def runtimeBindings(bindings: Map[String, Message]): Map[String,Message] = {
    val runtimeBindings = mutable.HashMap.empty[String,Message]
    /*
    for ((name, message) <- globalContext.externalStatics) {
      runtimeBindings.put(name, message)
    }
    */
    for ((name, message) <- bindings) {
      runtimeBindings.put(name, message)
    }
    runtimeBindings.toMap
  }

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

  def defaultSerializationOptions(contentType: MediaType): Map[QName,String] = {
    _defaultSerializationOptions.getOrElse(contentType.toString, Map.empty[QName,String])
  }

  def defaultSerializationOptions(contentType: String): Map[QName,String] = {
    _defaultSerializationOptions.getOrElse(contentType, Map.empty[QName,String])
  }

  protected[xmlcalabash] def setDefaultSerializationOptions(opts: Map[String,Map[QName,String]]): Unit = {
    _defaultSerializationOptions = opts
  }

  def trimInlineWhitespace: Boolean = _trim_inline_whitespace
  def trimInlineWhitespace_=(trim: Boolean): Unit = {
    _trim_inline_whitespace = trim
  }

  // ==============================================================================================

  def stepImplementation(staticContext: StaticContext): StepWrapper = {
    stepImplementation(staticContext, None)
  }

  def stepImplementation(staticContext: StaticContext, implParams: Option[ImplParams]): StepWrapper = {
    val stepType = staticContext.stepType
    val location = staticContext.location

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
