package com.xmlcalabash.steps

import java.net.URI

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, ValueUtils, XProcCollectionFinder}
import javax.xml.transform.{Result, SourceLocator}
import net.sf.saxon.Controller
import net.sf.saxon.lib.OutputURIResolver
import net.sf.saxon.s9api.{MessageListener, QName, ValidationMode, XdmDestination, XdmNode, XdmValue}
import net.sf.saxon.serialize.SerializationProperties

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Xslt extends DefaultXmlStep {
  private val _initial_mode = new QName("", "initial-mode")
  private val _template_name = new QName("", "template-name")
  private val _output_base_uri = new QName("", "output-base-uri")
  private val _version = new QName("", "version")
  private val _parameters = new QName("", "parameters")
  private val _content_type = new QName("", "content-type")
  private val cx_decode = new QName("cx", XProcConstants.ns_cx, "decode")

  private var stylesheet = Option.empty[XdmNode]
  private val defaultCollection = ListBuffer.empty[XdmNode]

  private var initialMode = Option.empty[QName]
  private var templateName = Option.empty[QName]
  private var outputBaseURI = Option.empty[String]
  private var parameters = Map.empty[QName, XdmValue]
  private var version = Option.empty[String]

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.EXACTLY_ONE, "stylesheet" -> PortCardinality.EXACTLY_ONE),
    Map("source" -> List("application/xml", "text/plain"), "stylesheet" -> List("application/xml"))
  )

  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.ZERO_OR_MORE, "secondary" -> PortCardinality.ZERO_OR_MORE),
    Map("result" -> List("application/xml", "text/plain"),
      "secondary" -> List("application/xml", "text/plain"))
  )

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    port match {
      case "source" => defaultCollection += item.asInstanceOf[XdmNode]
      case "stylesheet" => stylesheet = Some(item.asInstanceOf[XdmNode])
      case _ => Unit
    }
  }

  override def receiveBinding(variable: QName, value: XdmValue, context: ExpressionContext): Unit = {
    variable match {
      case `_initial_mode` => initialMode = Some(ValueParser.parseQName(ValueUtils.singletonStringValue(value, context.location), context.nsBindings, location))
      case `_template_name` => templateName = Some(ValueParser.parseQName(ValueUtils.singletonStringValue(value, context.location), context.nsBindings, location))
      case `_output_base_uri` => outputBaseURI = Some(ValueUtils.singletonStringValue(value, context.location))
      case `_parameters` => parameters = ValueParser.parseParameters(value, context.nsBindings, context.location)
      case `_version` => version = Some(ValueUtils.singletonStringValue(value, context.location))
      case _ =>
        logger.info("Ignoring unexpected option to p:xslt: " + variable)
    }
  }

  override def run(staticContext: StaticContext): Unit = {
    val document: Option[XdmNode] = defaultCollection.headOption

    if (version.isEmpty && stylesheet.isDefined) {
      val root = S9Api.documentElement(stylesheet.get)
      version = Option(root.get.getAttributeValue(_version))
    }

    val runtime = this.config.config
    val processor = runtime.processor
    val config = processor.getUnderlyingConfiguration
    // FIXME: runtime.getConfigurer().getSaxonConfigurer().configXSLT(config);


    val uriResolver = config.getOutputURIResolver
    val collectionFinder = config.getCollectionFinder
    val unparsedTextURIResolver = config.getUnparsedTextURIResolver

    config.setOutputURIResolver(new OutputResolver())
    config.setDefaultCollection(XProcCollectionFinder.DEFAULT)
    config.setCollectionFinder(new XProcCollectionFinder(runtime, defaultCollection.toList, collectionFinder))

    val compiler = processor.newXsltCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)
    val exec = compiler.compile(stylesheet.get.asSource())
    val transformer = exec.load()

    for ((param, value) <- parameters) {
      transformer.setParameter(param, value)
    }

    if (document.isDefined) {
      transformer.setInitialContextNode(document.get)
    }

    transformer.setMessageListener(new CatchMessages())

    val result = new XdmDestination()
    transformer.setDestination(result)

    if (initialMode.isDefined) {
      transformer.setInitialMode(initialMode.get)
    }

    if (templateName.isDefined) {
      transformer.setInitialTemplate(templateName.get)
    }

    if (outputBaseURI.isDefined) {
      transformer.setBaseOutputURI(outputBaseURI.get)
    }

    transformer.setSchemaValidationMode(ValidationMode.DEFAULT)
    // FIXME: transformer.getUnderlyingController().setUnparsedTextURIResolver(unparsedTextURIResolver)
    transformer.transform()

    // FIXME: try/finally restore the output URI resolver and collection URI finder

    val xformed = Option(result.getXdmNode)
    if (xformed.isDefined) {
      consumer.get.receive("result", xformed.get, new XProcMetadata(MediaType.XML))
    }
  }

  class OutputResolver extends OutputURIResolver {
    val secondaryResults = mutable.HashMap.empty[String, XdmDestination]

    override def newInstance: OutputURIResolver = {
      new OutputResolver
    }

    override def resolve(href: String, base: String): Result = {
      val baseURI = new URI(base).resolve(href)
      val xdmResult = new XdmDestination()

      secondaryResults.put(baseURI.toASCIIString, xdmResult)

      val controller = new Controller(config.processor.getUnderlyingConfiguration)
      val pipe = controller.makePipelineConfiguration()
      val receiver = xdmResult.getReceiver(pipe, new SerializationProperties())
      receiver.setSystemId(baseURI.toASCIIString)
      receiver
    }

    def close(result: Result): Unit = {
      val href = result.getSystemId
      val xdmResult = secondaryResults(href)
      val doc = xdmResult.getXdmNode
      consumer.get.receive("secondary", doc, new XProcMetadata(MediaType.XML))
    }
  }

  class CatchMessages extends MessageListener {
    override def message(content: XdmNode, terminate: Boolean, locator: SourceLocator): Unit = {
      val treeWriter = new SaxonTreeBuilder(config)
      treeWriter.startDocument(content.getBaseURI)
      treeWriter.addStartElement(XProcConstants.c_error)
      treeWriter.startContent()
      treeWriter.addSubtree(content)
      treeWriter.addEndElement()
      treeWriter.endDocument()

      // FIXME: step.reportError(treeWriter.getResult());
      // FIXME: step.info(step.getNode(), content.toString());
    }
  }

}
