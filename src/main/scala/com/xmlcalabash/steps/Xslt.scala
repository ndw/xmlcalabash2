package com.xmlcalabash.steps

import java.net.URI
import javax.xml.transform.{Result, SourceLocator}

import com.jafpl.messages.Metadata
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcCollectionFinder}
import com.xmlcalabash.model.xml.XProcConstants
import com.xmlcalabash.runtime.{S9Api, XmlMetadata, XmlPortSpecification}
import net.sf.saxon.lib.OutputURIResolver
import net.sf.saxon.s9api.{MessageListener, QName, ValidationMode, XdmDestination, XdmItem, XdmNode, XdmValue}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Xslt extends DefaultStep {
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
    Map("source" -> "1", "stylesheet" -> "1"),
    Map("source" -> List("application/xml", "text/plain"), "stylesheet" -> List("application/xml"))
  )

  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> "*", "secondary" -> "*"),
    Map("result" -> List("application/xml", "text/plain"),
      "secondary" -> List("application/xml", "text/plain"))
  )

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    port match {
      case "source" => defaultCollection += item.asInstanceOf[XdmNode]
      case "stylesheet" => stylesheet = Some(item.asInstanceOf[XdmNode])
      case _ => Unit
    }
  }

  override def receiveBinding(variable: QName, value: XdmItem, nsBindings: Map[String,String]): Unit = {
    variable match {
      case `_initial_mode` => initialMode = Some(lexicalQName(value.getStringValue, nsBindings))
      case `_template_name` => templateName = Some(lexicalQName(value.getStringValue, nsBindings))
      case `_output_base_uri` => outputBaseURI = Some(value.getStringValue)
      case `_parameters` => parameters = parseParameters(value, nsBindings)
      case `_version` => version = Some(value.getStringValue)
      case _ =>
        logger.info("Ignoring unexpected option to p:xslt: " + variable)
    }
  }

  override def run(): Unit = {
    val document: Option[XdmNode] = defaultCollection.headOption

    if (version.isEmpty && stylesheet.isDefined) {
      val root = S9Api.documentElement(stylesheet.get)
      version = Option(root.get.getAttributeValue(_version))
    }

    val runtime = this.config.get
    val processor = runtime.processor
    val config = this.config.get.processor.getUnderlyingConfiguration
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
      consumer.get.receive("result", xformed.get, new XmlMetadata("application/xml"))
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

      val receiver = xdmResult.getReceiver(config.get.processor.getUnderlyingConfiguration)
      receiver.setSystemId(baseURI.toASCIIString)
      receiver
    }

    def close(result: Result): Unit = {
      val href = result.getSystemId
      val xdmResult = secondaryResults(href)
      val doc = xdmResult.getXdmNode
      consumer.get.receive("secondary", doc, new XmlMetadata("application/xml"))
    }
  }

  class CatchMessages extends MessageListener {
    override def message(content: XdmNode, terminate: Boolean, locator: SourceLocator): Unit = {
      val treeWriter = new SaxonTreeBuilder(config.get)
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
