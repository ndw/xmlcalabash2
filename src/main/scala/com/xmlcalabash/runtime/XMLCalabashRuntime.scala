package com.xmlcalabash.runtime

import java.net.URI
import com.jafpl.config.Jafpl
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.Graph
import com.jafpl.messages.Message
import com.jafpl.runtime.{GraphRuntime, RuntimeConfiguration}
import com.jafpl.steps.DataConsumer
import com.jafpl.util.{ErrorListener, TraceEventManager}
import com.xmlcalabash.config.{DocumentManager, Signatures, XMLCalabashConfig, XProcConfigurer}
import com.xmlcalabash.exceptions.{ConfigurationException, ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{ExpressionParser, XProcConstants}
import com.xmlcalabash.model.xml.{Artifact, DeclareStep}
import com.xmlcalabash.util.stores.{DataStore, FallbackDataStore, FileDataStore, HttpDataStore}
import com.xmlcalabash.util.{MediaType, XProcVarValue}

import javax.xml.transform.URIResolver
import net.sf.saxon.lib.{ModuleURIResolver, UnparsedTextURIResolver}
import net.sf.saxon.s9api.{Processor, QName, XdmAtomicValue, XdmValue}
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.EntityResolver

import scala.collection.mutable

class XMLCalabashRuntime protected[xmlcalabash] (val decl: DeclareStep) extends RuntimeConfiguration {
  val config: XMLCalabashConfig = decl.config

  //FIXME: why?
  override def threadPoolSize: Int = config.threadPoolSize

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected[runtime] val joinGateMarker = new XdmAtomicValue(new QName(XProcConstants.ns_cx, "JOIN-GATE-MARKER"))

  private var _traceEventManager = config.traceEventManager
  private var _errorListener = config.errorListener
  private var _documentManager = config.documentManager
  private var _entityResolver = config.entityResolver
  private var _uriResolver = config.uriResolver
  private var _moduleURIResolver = config.moduleURIResolver
  private var _unparsedTextURIResolver = config.unparsedTextURIResolver
  private var _episode = config.computeEpisode
  private var _defaultSerializationOptions: Map[String,Map[QName,String]] = Map.empty[String,Map[QName,String]]
  private var _trim_inline_whitespace = config.trimInlineWhitespace
  private val inputSet = mutable.HashSet.empty[String]
  private val outputSet = mutable.HashSet.empty[String]
  private val bindingsMap = mutable.HashMap.empty[String,XdmValue]
  private val idMap = mutable.HashMap.empty[String,Artifact]
  private var ran = false
  private var _signatures: Signatures = _
  private var runtime: GraphRuntime = _
  private var _datastore = Option.empty[DataStore]

  val jafpl: Jafpl = Jafpl.newInstance()
  val graph: Graph = jafpl.newGraph()

  protected[xmlcalabash] def init(decl: DeclareStep): Unit = {
    try {
      config.debugOptions.dumpOpenGraph(decl, graph)

      graph.dumpGraphTransitions = true

      runtime = new GraphRuntime(graph, this)
      config.debugOptions.dumpJafplGraph(decl, graph)
      runtime.traceEventManager = _traceEventManager
    } catch {
      case ex: JafplException =>
        ex.code match {
          case JafplException.BAD_LOOP_INPUT_PORT =>
            throw XProcException.xsLoop(ex.details(1).asInstanceOf[String], ex.details.head.asInstanceOf[String], ex.location)
          case _ =>
            throw ex
        }
      case ex: Throwable =>
        throw ex
    }
  }

  // ===================================================================================

  def inputs: List[String] = decl.inputPorts
  def outputs: List[String] = decl.outputPorts

  protected[runtime] def inputMessage(port: String, msg: Message): Unit = {
    runtime.inputs(port).send(msg)
  }

  def input(port: String, item: XdmValue, metadata: XProcMetadata): Unit = {
    if (runtime.inputs.contains(port)) {
      if (inputSet.isEmpty) {
        // If there are any defaults for this input port, disable them
        decl.input(port).disableDefaults()
      }
      inputSet += port
      runtime.inputs(port).send(new XdmValueItemMessage(item, metadata, new StaticContext(this)))
    }
  }

  def input(port: String, message: Message): Unit = {
    if (runtime.inputs.contains(port)) {
      if (inputSet.isEmpty) {
        // If there are any defaults for this input port, disable them
        decl.input(port).disableDefaults()
      }
      inputSet += port
      runtime.inputs(port).send(message)
    }
  }

  def output(port: String, consumer: DataConsumer): Unit = {
    val dcp = runtime.outputs(port)
    dcp.setConsumer(consumer)
  }

  def serializationOptions(port: String): Map[QName,String] = {
    decl.output(port).serialization
  }

  def option(name: QName, value: String): Unit = {
    option(name, new XProcVarValue(new XdmAtomicValue(value), new StaticContext(this)))
  }

  def option(name: QName, value: Integer): Unit = {
    option(name, new XProcVarValue(new XdmAtomicValue(value), new StaticContext(this)))
  }

  def option(name: QName, value: Float): Unit = {
    option(name, new XProcVarValue(new XdmAtomicValue(value), new StaticContext(this)))
  }

  def option(name: QName, value: URI): Unit = {
    option(name, new XProcVarValue(new XdmAtomicValue(value), new StaticContext(this)))
  }

  def option(name: QName, value: XProcVarValue): Unit = {
    if (decl.bindings.contains(name)) {
      config.trace(s"Binding option $name to '$value'", "ExternalBindings")
      //val msg = new XdmValueItemMessage(value.value, XProcMetadata.XML, value.context)
      //runtime.setOption(name.getClarkName, value)
      bindingsMap.put(name.getClarkName, value.value)
    }
  }

  def run(): Unit = {
    if (ran) {
      throw new RuntimeException("You must call reset() before running a pipeline a second time.")
    }

    runCommon()
  }

  private def runCommon(): Unit = {
    ran = true

    try {
      runtime.runSync()
    } finally {
      runtime.stop()
    }
  }

  def reset(): Unit = {
    /*
    //_staticOptionBindings.clear()
    inputSet.clear()
    outputSet.clear()
    bindingsMap.clear()

    _graph = decl.pipelineGraph()
    _graph.close()
    runtime = new GraphRuntime(_graph, this)
    runtime.traceEventManager = _traceEventManager
    ran = false
     */
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

  def datastore: DataStore = {
    if (_datastore.isEmpty) {
      val fallback = new FallbackDataStore()
      val filestore = new FileDataStore(config, fallback)
      _datastore = Some(new HttpDataStore(config, filestore))
    }

    _datastore.get
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

  def stepImplementation(stepType: QName, staticContext: StaticContext): StepWrapper = {
    stepImplementation(stepType, staticContext, None)
  }

  def stepImplementation(stepType: QName, staticContext: StaticContext, implParams: Option[ImplParams]): StepWrapper = {
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
