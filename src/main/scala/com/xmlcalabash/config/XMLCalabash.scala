package com.xmlcalabash.config

import java.net.URI

import javax.xml.transform.URIResolver
import javax.xml.transform.sax.SAXSource
import com.jafpl.graph.Location
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.{ExpressionEvaluator, RuntimeConfiguration}
import com.jafpl.steps.{DataConsumer, Step}
import com.jafpl.util.{ErrorListener, TraceEventManager}
import com.xmlcalabash.exceptions.{ConfigurationException, ExceptionCode, ModelException}
import com.xmlcalabash.functions.{Cwd, DocumentProperties, DocumentPropertiesDocument, DocumentProperty, ForceQNameKeys, InjElapsed, InjId, InjName, InjType, SystemProperty}
import com.xmlcalabash.model.util.ExpressionParser
import com.xmlcalabash.model.xml.Artifact
import com.xmlcalabash.parsers.XPathParser
import com.xmlcalabash.runtime.{ImplParams, SaxonExpressionEvaluator, StepWrapper, XmlStep}
import com.xmlcalabash.sbt.BuildInfo
import com.xmlcalabash.util.{URIUtils, XProcURIResolver}
import net.sf.saxon.lib.{ExtensionFunctionDefinition, ModuleURIResolver, UnparsedTextURIResolver}
import net.sf.saxon.s9api.{Processor, QName, XdmNode, XdmValue}
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.helpers.XMLReaderFactory
import org.xml.sax.{EntityResolver, InputSource}

import scala.collection.mutable

object XMLCalabash {
  val _configProperty = "com.xmlcalabash.config.XMLCalabashConfigurer"
  val _configClass = "com.xmlcalabash.util.DefaultXMLCalabashConfigurer"

  def newInstance(): XMLCalabash = {
    val configurer = Class.forName(configClass).newInstance()
    val config = new XMLCalabash()
    configurer.asInstanceOf[XMLCalabashConfigurer].configure(config)
    config.close()
    config
  }

  private def configClass: String = Option(System.getProperty(_configProperty)).getOrElse(_configClass)
}

class XMLCalabash extends RuntimeConfiguration {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val _expressionEvaluator = new SaxonExpressionEvaluator(this)
  private val _collections = mutable.HashMap.empty[String, List[XdmNode]]
  private val idMap = mutable.HashMap.empty[String,Artifact]
  private val staticOptionBindings = mutable.HashMap.empty[QName, XdmValue]

  private var closed = false
  private var _processor: Processor = _
  private var _errorListener: ErrorListener = _
  private var _stepImplClasses = mutable.HashMap.empty[QName,String]
  private var _funcImplClasses = mutable.HashMap.empty[QName,String]
  private var _signatures: Signatures = _
  private var _traceEventManager: TraceEventManager = _
  private var _uriResolver: URIResolver = _
  private var _entityResolver: EntityResolver = _
  private var _moduleURIResolver: ModuleURIResolver = _
  private var _unparsedTextURIResolver: UnparsedTextURIResolver = _
  private var _errorExplanation: ErrorExplanation = _
  private var _documentManager: DocumentManager = _
  private var _htmlSerializer = false
  private var _watchdogTimeout = 1000L
  private var _staticBaseURI = URIUtils.cwdAsURI
  private var _language = defaultLocale
  private var _episode = defaultEpisode

  def productName: String = BuildInfo.name
  def productVersion: String = BuildInfo.version
  def jafplVersion: String = BuildInfo.jafplVersion
  def saxonVersion: String = {
    val sver = processor.getSaxonProductVersion
    val sed = processor.getUnderlyingConfiguration.getEditionCode
    s"$sver/$sed"
  }
  def productConfig: String = {
    s"${BuildInfo.version} (with JAFPL $jafplVersion for Saxon $saxonVersion)"
  }
  def vendor: String = "Norman Walsh"
  def vendorURI: String = "http://xmlcalabash.com/"
  def xprocVersion: String = "3.0"
  def xpathVersion: String = "3.1"
  def psviSupported: Boolean = processor.isSchemaAware

  def processor: Processor = {
    if (_processor == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "processor")
    }
    _processor
  }
  def processor_=(proc: Processor): Unit = {
    checkClosed()
    _processor = proc
  }

  def errorListener: ErrorListener = {
    if (_errorListener == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "errorListener")
    }
    _errorListener
  }
  def errorListener_=(listener: ErrorListener): Unit = {
    checkClosed()
    _errorListener = listener
  }

  def signatures: Signatures = {
    if (_signatures == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "signatures")
    }
    _signatures
  }
  def signatures_=(signatures: Signatures): Unit = {
    checkClosed()
    _signatures = signatures
  }

  def traceEventManager: TraceEventManager = {
    if (_traceEventManager == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "traceEventManager")
    }
    _traceEventManager
  }
  def traceEventManager_=(manager: TraceEventManager): Unit = {
    checkClosed()
    _traceEventManager = manager
  }

  def uriResolver: URIResolver = {
    if (_uriResolver == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "uriResolver")
    }
    _uriResolver
  }
  def uriResolver_=(resolver: URIResolver): Unit = {
    checkClosed()
    _uriResolver = resolver
  }

  def entityResolver: EntityResolver = {
    if (_entityResolver == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "entityResolver")
    }
    _entityResolver
  }
  def entityResolver_=(resolver: EntityResolver): Unit = {
    checkClosed()
    _entityResolver = resolver
  }

  def moduleURIResolver: ModuleURIResolver = {
    if (_moduleURIResolver == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "moduleURIResolver")
    }
    _moduleURIResolver
  }
  def moduleURIResolver_=(resolver: ModuleURIResolver): Unit = {
    checkClosed()
    _moduleURIResolver = resolver
  }

  def unparsedTextURIResolver: UnparsedTextURIResolver = {
    if (_unparsedTextURIResolver == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "uparsedTextURIResolver")
    }
    _unparsedTextURIResolver
  }
  def unparsedTextURIResolver_=(resolver: UnparsedTextURIResolver): Unit = {
    checkClosed()
    _unparsedTextURIResolver = resolver
  }

  def errorExplanation: ErrorExplanation = {
    if (_errorExplanation == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "errorExplanation")
    }
    _errorExplanation
  }
  def errorExplanation_=(explain: ErrorExplanation): Unit = {
    checkClosed()
    _errorExplanation = explain
  }

  def documentManager: DocumentManager = {
    if (_documentManager == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "documentManager")
    }
    _documentManager
  }
  def documentManager_=(manager: DocumentManager): Unit = {
    checkClosed()
    _documentManager = manager
  }

  def htmlSerializer: Boolean = _htmlSerializer
  def htmlSerializer_=(html: Boolean): Unit = {
    checkClosed()
    _htmlSerializer = html
  }

  def watchdogTimeout: Long = _watchdogTimeout
  def watchdogTimeout_=(timeout: Long): Unit = {
    checkClosed()
    _watchdogTimeout = timeout
  }

  def language: String = _episode
  def language_=(language: String): Unit = {
    checkClosed()
    // FIXME: Check for valid format
    _language = language
  }

  def staticBaseURI: URI = _staticBaseURI
  def staticBaseURI_=(uri: URI): Unit = {
    checkClosed()
    if (uri.isAbsolute) {
      _staticBaseURI = uri
    } else {
      throw new ConfigurationException(ExceptionCode.MUSTBEABS, "staticBaseURI")
    }
  }

  def episode: String = _episode
  def episode_=(episode: String): Unit = {
    checkClosed()
    _episode = episode
  }

  def staticOptionValue(option: QName): Option[XdmValue] = staticOptionBindings.get(option)
  def setStaticOptionValue(option: QName, value: XdmValue): Unit = {
    staticOptionBindings.put(option, value)
  }

  // ==============================================================================================

  def implementFunction(funcName: QName, className: String): Unit = {
    if (_funcImplClasses.contains(funcName)) {
      throw new RuntimeException("You cannot redefine a function implementation class")
    }
    _funcImplClasses.put(funcName, className)
  }

  def functionImplementation(funcName: QName): Option[String] = _funcImplClasses.get(funcName)

  def implementAtomicStep(stepType: QName, className: String): Unit = {
    if (_stepImplClasses.contains(stepType)) {
      throw new RuntimeException("You cannot redefine a step implementation class")
    }
    _stepImplClasses.put(stepType, className)
  }

  def atomicStepImplementation(stepType: QName): Option[String] = _stepImplClasses.get(stepType)

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

  // FIXME: Should this be a factory, or should XPathParser be reusable?
  def expressionParser: ExpressionParser = {
    new XPathParser(this)
  }

  override def expressionEvaluator: SaxonExpressionEvaluator = _expressionEvaluator

  override def traceEnabled(trace: String): Boolean = traceEventManager.traceEnabled(trace)

  // Convenience functions
  def trace(message: String, event:String): Unit = traceEventManager.trace(message,event)
  def trace(level: String, message: String, event:String): Unit = traceEventManager.trace(level,message,event)

  // ==============================================================================================

  def setCollection(href: URI, docs: List[XdmNode]): Unit = {
    _collections.put(href.toASCIIString, docs)
  }

  def collection(href: URI): List[XdmNode] = {
    _collections.getOrElse(href.toASCIIString, List.empty[XdmNode])
  }

  // ==============================================================================================

  private def defaultLocale: String = {
    import java.util.Locale

    // Translate _ to - for compatibility with xml:lang
    Locale.getDefault.toString.replace('_', '-')
  }

  private def defaultEpisode: String = {
    import java.security.MessageDigest
    import java.util.GregorianCalendar

    val digest = MessageDigest.getInstance("MD5")
    val calendar = new GregorianCalendar()

    val hash = digest.digest(calendar.toString.getBytes)
    var episode = "CB"
    for (b <- hash) {
      episode = episode + Integer.toHexString(b & 0xff)
    }

    episode
  }

  // ==============================================================================================

  def close(): Unit = {
    closed = true
    // This doesn't work because I don't know how to dynamically call the constructor that has an argument
    /*
    for (xf <- signatures.functions) {
      val impl = signatures.function(xf).head
      trace("debug", s"Registering $xf with implementation $impl", "config")
      println(s"Registering $xf with implementation $impl")
      val f = Class.forName(impl).newInstance()
      processor.registerExtensionFunction(f.asInstanceOf[ExtensionFunctionDefinition])
    }
    */
    processor.registerExtensionFunction(new DocumentProperties(this))
    processor.registerExtensionFunction(new DocumentProperty(this))
    processor.registerExtensionFunction(new DocumentPropertiesDocument(this))
    processor.registerExtensionFunction(new ForceQNameKeys(this))
    processor.registerExtensionFunction(new SystemProperty(this))
    processor.registerExtensionFunction(new Cwd(this))
    processor.registerExtensionFunction(new InjElapsed(this))
    processor.registerExtensionFunction(new InjName(this))
    processor.registerExtensionFunction(new InjId(this))
    processor.registerExtensionFunction(new InjType(this))
  }
  private def checkClosed(): Unit = {
    if (closed) {
      throw new ConfigurationException(ExceptionCode.CLOSED, "XMLCalabash")
    }
  }

  // ==============================================================================================

  def parse(uri: String, base: URI): XdmNode = {
    parse(uri, base, validate=false)
  }

  def parse(uri: String, base: URI, validate: Boolean): XdmNode = {
    val href = URIUtils.encode(uri)
    logger.debug("Attempting to parse: " + uri)

    var source = uriResolver.resolve(href, base.toASCIIString)
    if (source == null) {
      var resURI = base.resolve(href)
      val path = resURI.toASCIIString
      val pos = path.indexOf("!")
      if (pos > 0 && (path.startsWith("jar:file:") || path.startsWith("jar:http:") || path.startsWith("jar:https:"))) {
        // You can't resolve() against jar: scheme URIs because they appear to be opaque.
        // I wonder if what follows is kosher...
        var fakeURIstr = "http://example.com"
        val subpath = path.substring(pos + 1)
        if (subpath.startsWith("/")) {
          fakeURIstr += subpath
        } else {
          fakeURIstr += "/" + subpath
        }
        val fakeURI = new URI(fakeURIstr)
        resURI = fakeURI.resolve(href)
        fakeURIstr = path.substring(0, pos + 1) + resURI.getPath
        resURI = new URI(fakeURIstr)
      }

      source = new SAXSource(new InputSource(resURI.toASCIIString))
      var reader = source.asInstanceOf[SAXSource].getXMLReader
      if (reader == null) {
        reader = XMLReaderFactory.createXMLReader
        source.asInstanceOf[SAXSource].setXMLReader(reader)
        reader.setEntityResolver(entityResolver)
      }
    }

    val builder = processor.newDocumentBuilder()
    builder.setDTDValidation(validate)
    builder.setLineNumbering(true)

    builder.build(source)
  }

  // ==============================================================================================

  def addNode(id: String, artifact: Artifact): Unit = {
    idMap.put(id, artifact)
  }

  def node(id: String): Option[Artifact] = idMap.get(id)

}
