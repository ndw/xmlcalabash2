package com.xmlcalabash.steps

import java.net.URI

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, XProcCollectionFinder}
import javax.xml.transform.{ErrorListener, SourceLocator, TransformerException}
import net.sf.saxon.event.{PipelineConfiguration, Receiver}
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.functions.ResolveURI
import net.sf.saxon.lib.{ResultDocumentResolver, SaxonOutputKeys}
import net.sf.saxon.s9api.{Destination, MessageListener, QName, RawDestination, ValidationMode, XdmAtomicValue, XdmDestination, XdmItem, XdmNode, XdmNodeKind, XdmValue}
import net.sf.saxon.serialize.SerializationProperties

import scala.collection.JavaConverters._
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

  private var primaryDestination: Destination = _
  private val secondaryResults = mutable.HashMap.empty[URI, Destination]
  private val secondaryOutputProperties = mutable.HashMap.empty[URI, Map[QName, XdmValue]]

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.ZERO_OR_MORE, "stylesheet" -> PortCardinality.EXACTLY_ONE),
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

    if (version.isEmpty || !List("1.0","2.0","3.0").contains(version.get)) {
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
    //transformer.setResultDocumentHandler(new ResultDocumentHandler())
    transformer.getUnderlyingController.setResultDocumentResolver(new MyResultDocumentResolver)

    transformer.setDestination(new MyDestination(outputProperties))

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

    try {
      transformer.transform()
    } catch {
      case sae: Exception =>
        // Compile time exceptions are caught
        if (goesBang.isDefined) {
          throw goesBang.get
        }
        // Runtime ones are not
        if (sae.getMessage.contains("terminated by xsl:message")) {
          throw XProcException.xcXsltUserTermination(sae.getMessage, location)
        } else {
          throw XProcException.xcXsltRuntimeError(sae.getMessage, location)
        }
    }

    // FIXME: try/finally restore the output URI resolver and collection URI finder

    primaryDestination match {
      case raw: RawDestination =>
        val iter = raw.getXdmValue.iterator()
        while (iter.hasNext) {
          consume(iter.next(), "result", outputProperties.toMap)
        }
      case xdm: XdmDestination =>
        consume(xdm.getXdmNode, "result",  outputProperties.toMap)
    }

    for ((uri, destination) <- secondaryResults) {
      val props = secondaryOutputProperties(uri)
      destination match {
        case raw: RawDestination =>
          val iter = raw.getXdmValue.iterator()
          while (iter.hasNext) {
            consume(iter.next(), "secondary", props + Tuple2(XProcConstants._base_uri, new XdmAtomicValue(uri)))
          }
        case xdm: XdmDestination =>
          consume(xdm.getXdmNode, "secondary", props + Tuple2(XProcConstants._base_uri, new XdmAtomicValue(uri)))
      }
    }
  }

  override def reset(): Unit = {
    super.reset()
    goesBang = None
  }

  private def consume(item: XdmItem, port: String, prop: Map[QName,XdmValue]): Unit = {
    var outputItem = item
    var ctype = MediaType.JSON // items are JSON by default

    item match {
      case node: XdmNode =>
        node.getNodeKind match {
          case XdmNodeKind.TEXT =>
            ctype = MediaType.TEXT // or nodes
          case _ =>
            ctype = MediaType.XML // of any sort
        }

        if (node.getNodeKind != XdmNodeKind.DOCUMENT) {
          val builder = new SaxonTreeBuilder(config)
          builder.startDocument(node.getBaseURI)
          builder.addSubtree(node)
          builder.endDocument()
          outputItem = builder.result
        }

      case _: XdmAtomicValue =>
        // nop; atomic values are JSON

      // explicitly not catching _ because I want anything else to fail
    }

    consumer.get.receive(port, outputItem, new XProcMetadata(ctype, prop))
  }

  private class MyDestination(map: mutable.HashMap[QName,XdmValue]) extends RawDestination {
    private var destination = Option.empty[Destination]
    private var destBase = Option.empty[URI]

    override def setDestinationBaseURI(baseURI: URI): Unit = {
      destBase = Some(baseURI)
      if (destination.isDefined) {
        destination.get.setDestinationBaseURI(baseURI)
      }
    }

    override def getDestinationBaseURI: URI = destBase.getOrElse(null)

    override def getReceiver(pipe: PipelineConfiguration, params: SerializationProperties): Receiver = {
      val tree = Option(params.getProperty(SaxonOutputKeys.BUILD_TREE))

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

      val dest = if (tree.getOrElse("no") == "yes") {
        new XdmDestination()
      } else {
        new RawDestination()
      }

      if (destBase.isDefined) {
        dest.setDestinationBaseURI(destBase.get)
      }

      destination = Some(dest)
      primaryDestination = dest
      dest.getReceiver(pipe, params)
    }

    override def closeAndNotify(): Unit = {
      if (destination.isDefined) {
        destination.get.closeAndNotify()
      }
    }

    override def close(): Unit = {
      if (destination.isDefined) {
        destination.get.close()
      }
    }
  }

  class MyResultDocumentResolver extends ResultDocumentResolver {
    override def resolve(context: XPathContext, href: String, baseUri: String, properties: SerializationProperties): Receiver = {
      val tree = Option(properties.getProperty(SaxonOutputKeys.BUILD_TREE))
      val uri = ResolveURI.makeAbsolute(href, baseUri)
      val destination = if (tree.getOrElse("no") == "yes") {
        new XdmDestination()
      } else {
        new RawDestination()
      }

      val xprocProps = mutable.HashMap.empty[QName, XdmValue]
      for (rawkey <- properties.getProperties.keySet().asScala) {
        val key = rawkey.toString
        println(key)
      }

      secondaryOutputProperties.put(uri, xprocProps.toMap)
      secondaryResults.put(uri, destination)

      destination.getReceiver(context.getReceiver.getPipelineConfiguration, properties);
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
