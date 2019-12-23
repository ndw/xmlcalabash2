package com.xmlcalabash.steps

import java.net.URI

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, XProcCollectionFinder}
import javax.xml.transform.{ErrorListener, SourceLocator, TransformerException}
import net.sf.saxon.event.{PipelineConfiguration, Receiver}
import net.sf.saxon.s9api.{Destination, MessageListener, QName, ValidationMode, XdmAtomicValue, XdmDestination, XdmNode, XdmValue}
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.trans.XPathException

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Xslt extends DefaultXmlStep {
  private val _initial_mode = new QName("", "initial-mode")
  private val _template_name = new QName("", "template-name")
  private val _output_base_uri = new QName("", "output-base-uri")

  private var stylesheet = Option.empty[XdmNode]
  private val defaultCollection = ListBuffer.empty[XdmNode]

  private var initialMode = Option.empty[QName]
  private var templateName = Option.empty[QName]
  private var outputBaseURI = Option.empty[String]
  private var parameters = Map.empty[QName, XdmValue]
  private var version = Option.empty[String]

  private var goesBang = Option.empty[XProcException]
  private val outputProperties = mutable.HashMap.empty[QName, XdmValue]

  private val secondaryResults = mutable.HashMap.empty[URI, XdmDestination]
  private val secondaryOutputProperties = mutable.HashMap.empty[URI, mutable.HashMap[QName, XdmValue]]

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

  override def receiveBinding(variable: QName, value: XdmValue, context: StaticContext): Unit = {
    if (variable == XProcConstants._parameters) {
      if (value.size() > 0) {
        parameters = ValueParser.parseParameters(value, context)
      }
    } else {
      // All of the other options should be single values or the empty sequence
      value.size() match {
        case 0 => Unit
        case 1 =>
          val str = value.getUnderlyingValue.getStringValue
          variable match {
            case `_initial_mode` => initialMode = Some(ValueParser.parseQName(str, context))
            case `_template_name` => templateName = Some(ValueParser.parseQName(str, context))
            case `_output_base_uri` => outputBaseURI = Some(str)
            case XProcConstants._version => version = Some(str)
            case _ =>
              logger.info("Ignoring unexpected option to p:xslt: " + variable)
          }
        case _ =>
          throw new RuntimeException(s"The value of $variable may not be a sequence")
      }
    }
  }

  override def run(staticContext: StaticContext): Unit = {
    val document: Option[XdmNode] = defaultCollection.headOption

    if (version.isEmpty && stylesheet.isDefined) {
      val root = S9Api.documentElement(stylesheet.get)
      version = Option(root.get.getAttributeValue(XProcConstants._version))
    }

    if (version.isEmpty || (version.get != "2.0" && version.get != "3.0")) {
      throw XProcException.xcVersionNotAvailable(version.getOrElse(""), location)
    }

    val runtime = this.config.config
    val processor = runtime.processor
    val config = processor.getUnderlyingConfiguration
    // FIXME: runtime.getConfigurer().getSaxonConfigurer().configXSLT(config);


    val collectionFinder = config.getCollectionFinder
    val unparsedTextURIResolver = config.getUnparsedTextURIResolver

    config.setDefaultCollection(XProcCollectionFinder.DEFAULT)
    config.setCollectionFinder(new XProcCollectionFinder(runtime, defaultCollection.toList, collectionFinder))

    val compiler = processor.newXsltCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)
    compiler.setErrorListener(new MyErrorListener(true))
    val exec = try {
      compiler.compile(stylesheet.get.asSource())
    } catch {
      case e: Exception =>
        throw goesBang.getOrElse(e)
    }
    val transformer = exec.load()

    for ((param, value) <- parameters) {
      transformer.setParameter(param, value)
    }

    if (document.isDefined) {
      transformer.setInitialContextNode(document.get)
    }

    transformer.setMessageListener(new CatchMessages())
    transformer.setResultDocumentHandler(new ResultDocumentHandler())

    val result = new MyDestination(new XdmDestination(), outputProperties)
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
      consumer.get.receive("result", xformed.get, new XProcMetadata(MediaType.XML, outputProperties.toMap))
    }

    for ((uri, destination) <- secondaryResults) {
      val props = secondaryOutputProperties(uri)
      consumer.get.receive("secondary", destination.getXdmNode, new XProcMetadata(MediaType.XML, props.toMap))
    }
  }

  override def reset(): Unit = {
    super.reset()
    goesBang = None
  }

  private class MyDestination(val destination: XdmDestination, map: mutable.HashMap[QName,XdmValue]) extends XdmDestination {
    override def setDestinationBaseURI(baseURI: URI): Unit = {
      destination.setDestinationBaseURI(baseURI)
    }

    override def getDestinationBaseURI: URI = destination.getDestinationBaseURI

    override def getReceiver(pipe: PipelineConfiguration, params: SerializationProperties): Receiver = {
      val props = params.getProperties
      val enum = props.propertyNames()
      while (enum.hasMoreElements) {
        val name: String = enum.nextElement().asInstanceOf[String]
        val qname = if (name.startsWith("{")) {
          ValueParser.parseClarkName(name)
        } else {
          new QName(name)
        }
        val value = props.get(name).asInstanceOf[String]
        if (value == "yes" || value == "no") {
          map.put(qname, new XdmAtomicValue(value == "yes"))
        } else {
          map.put(qname, new XdmAtomicValue(value))
        }
      }
      destination.getReceiver(pipe, params)
    }

    override def closeAndNotify(): Unit = {
      destination.closeAndNotify()
    }

    override def close(): Unit = {
      destination.close()
    }

    override def getXdmNode: XdmNode = {
      destination.getXdmNode
    }
  }

  class ResultDocumentHandler extends java.util.function.Function[URI,Destination] {
    override def apply(uri: URI): Destination = {
      val destination = new XdmDestination()
      val outputProps = mutable.HashMap.empty[QName, XdmValue]
      secondaryOutputProperties.put(uri, outputProps)
      secondaryResults.put(uri, new MyDestination(destination, outputProps))
      destination
    }
  }

  private class CatchMessages extends MessageListener {
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

  private class MyErrorListener(val compileTime: Boolean) extends ErrorListener {
    override def warning(e: TransformerException): Unit = Unit

    override def error(e: TransformerException): Unit = {
      goesBang = Some(XProcException.xcXsltCompileError(e.getMessage, location))
    }

    override def fatalError(e: TransformerException): Unit = {
      goesBang = Some(XProcException.xcXsltCompileError(e.getMessage, location))
    }
  }
}
