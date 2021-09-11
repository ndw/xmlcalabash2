package com.xmlcalabash.steps

import java.net.URI
import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, XProcCollectionFinder}
import net.sf.saxon.Configuration

import javax.xml.transform.{ErrorListener, SourceLocator, TransformerException}
import net.sf.saxon.event.{PipelineConfiguration, Receiver}
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.functions.ResolveURI
import net.sf.saxon.lib.{ResultDocumentResolver, SaxonOutputKeys}
import net.sf.saxon.s9api.{Axis, Destination, MessageListener, QName, RawDestination, ValidationMode, XdmArray, XdmAtomicValue, XdmDestination, XdmEmptySequence, XdmItem, XdmMap, XdmNode, XdmNodeKind, XdmValue}
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.trans.XPathException

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{IteratorHasAsJava, MapHasAsJava, SetHasAsScala}

class Xslt extends DefaultXmlStep {
  private val _global_context_item = new QName("", "global-context-item")
  private val _initial_mode = new QName("", "initial-mode")
  private val _template_name = new QName("", "template-name")
  private val _output_base_uri = new QName("", "output-base-uri")

  private var stylesheet = Option.empty[XdmNode]
  private val inputSequence = ListBuffer.empty[XdmItem]

  private var globalContextItem = Option.empty[XdmValue]
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
      case "source" => inputSequence += item.asInstanceOf[XdmItem]
      case "stylesheet" => stylesheet = Some(item.asInstanceOf[XdmNode])
      case _ => ()
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
        case 0 => ()
        case 1 =>
          if (variable == _global_context_item) {
            globalContextItem = Some(value)
          } else {
            val str = value.getUnderlyingValue.getStringValue
            variable match {
              case `_initial_mode` => initialMode = Some(ValueParser.parseQName(str, context))
              case `_template_name` => templateName = Some(ValueParser.parseQName(str, context))
              case `_output_base_uri` => outputBaseURI = Some(str)
              case XProcConstants._version => version = Some(str)
              case _ =>
                logger.info("Ignoring unexpected option to p:xslt: " + variable)
            }
          }
        case _ =>
          throw new RuntimeException(s"The value of $variable may not be a sequence")
      }
    }
  }

  override def run(staticContext: StaticContext): Unit = {
    val document: Option[XdmValue] = inputSequence.headOption

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

    if (templateName.isDefined || version.get != "3.0") {
      config.setDefaultCollection(XProcCollectionFinder.DEFAULT)
      val docs = ListBuffer.empty[XdmNode]
      for (value <- inputSequence) {
        value match {
          case node: XdmNode => docs += node
          case _ => ()
        }
      }
      config.setCollectionFinder(new XProcCollectionFinder(runtime, docs.toList, collectionFinder))
    }

    val compiler = processor.newXsltCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)

    val exec = try {
      compiler.compile(stylesheet.get.asSource())
    } catch {
      case e: Exception =>
        throw goesBang.getOrElse(e)
    }
    val transformer = exec.load30()

    transformer.setStylesheetParameters(parameters.asJava)

    val inputSelection = if (document.isDefined) {
      val iter = inputSequence.iterator.asJava
      new XdmValue(iter)
    } else {
      XdmEmptySequence.getInstance
    }

    transformer.setMessageListener(new CatchMessages())


    //transformer.setResultDocumentHandler(new ResultDocumentHandler())
    //transformer.setDestination(new MyDestination(outputProperties))
    transformer.getUnderlyingController.setResultDocumentResolver(new MyResultDocumentResolver(processor.getUnderlyingConfiguration))

    if (initialMode.isDefined) {
      try {
        transformer.setInitialMode(initialMode.get)
      } catch {
        case iae: IllegalArgumentException =>
          throw XProcException.xcXsltNoMode(initialMode.get, iae.getMessage, location)
      }
    }

    if (outputBaseURI.isDefined) {
      if (staticContext.baseURI.isDefined) {
        transformer.setBaseOutputURI(staticContext.baseURI.get.resolve(outputBaseURI.get).toASCIIString)
      } else {
        transformer.setBaseOutputURI(outputBaseURI.get)
      }
    } else {
      if (inputSequence.nonEmpty && inputSequence.head.isInstanceOf[XdmNode]) {
        val base = inputSequence.head.asInstanceOf[XdmNode].getBaseURI
        transformer.setBaseOutputURI(base.toASCIIString)
      }
    }

    transformer.setSchemaValidationMode(ValidationMode.DEFAULT)
    // FIXME: transformer.getUnderlyingController().setUnparsedTextURIResolver(unparsedTextURIResolver)

    try {
      val destination = new MyDestination(outputProperties)
      if (globalContextItem.isDefined) {
        transformer.setGlobalContextItem(globalContextItem.get.asInstanceOf[XdmItem])
      }
      if (templateName.isDefined) {
        transformer.callTemplate(templateName.get, destination)
      } else {
        transformer.applyTemplates(inputSelection, destination)
      }
    } catch {
      case sae: Exception =>
        // Compile time exceptions are caught
        if (goesBang.isDefined) {
          throw goesBang.get
        }
        // Runtime ones are not
        val cause = if (Option(sae.getCause).isDefined) {
          sae.getCause match {
            case xe: XPathException =>
              Some(new QName(xe.getErrorCodeNamespace, xe.getErrorCodeLocalPart))
            case _ =>
              None
          }
        } else {
          None
        }

        if (cause.isDefined) {
          cause.get match {
            case XProcConstants.err_XTMM9000 =>
              throw XProcException.xcXsltUserTermination(sae.getMessage, location)
            case XProcConstants.err_XTDE0040 =>
              throw XProcException.xcXsltNoTemplate(templateName.get, location)
            case _ =>
              throw XProcException.xcXsltRuntimeError(cause.get, sae.getMessage, location)
          }
        }

        throw XProcException.xcXsltRuntimeError(XProcConstants.err_XC0095, sae.getMessage, location)
    }

    // FIXME: try/finally restore the output URI resolver and collection URI finder

    primaryDestination match {
      case raw: RawDestination =>
        val iter = raw.getXdmValue.iterator()
        while (iter.hasNext) {
          val next = iter.next()
          val prop = outputProperties.clone()
          next match {
            case node: XdmNode =>
              prop(XProcConstants._base_uri) = new XdmAtomicValue(node.getBaseURI)
            case _ => ()
          }
          consume(next, "result", prop.toMap)
        }
      case xdm: XdmDestination =>
        val tree = xdm.getXdmNode
        val prop = outputProperties.clone()
        prop(XProcConstants._base_uri) = new XdmAtomicValue(tree.getBaseURI)
        consume(tree, "result",  prop.toMap)
    }

    for ((uri, destination) <- secondaryResults) {
      val props = secondaryOutputProperties(uri)
      destination match {
        case raw: RawDestination =>
          val iter = raw.getXdmValue.iterator()
          while (iter.hasNext) {
            val next = iter.next()
            consume(next, "secondary", props + Tuple2(XProcConstants._base_uri, new XdmAtomicValue(uri)))
          }
        case xdm: XdmDestination =>
          val tree = xdm.getXdmNode
          consume(tree, "secondary", props + Tuple2(XProcConstants._base_uri, new XdmAtomicValue(uri)))
      }
    }
  }

  override def reset(): Unit = {
    super.reset()
    goesBang = None
  }

  private def consume(item: XdmItem, port: String, sprop: Map[QName,XdmValue]): Unit = {
    var outputItem = item
    var ctype = Option.empty[MediaType]

    var serialization = new XdmMap()
    for ((key, value) <- sprop) {
      serialization = serialization.put(new XdmAtomicValue(key), value)
    }

    val dprop = mutable.HashMap.empty[QName, XdmValue]
    dprop.put(XProcConstants._serialization, serialization)

    if (sprop.contains(XProcConstants._method)) {
      sprop(XProcConstants._method).toString match {
        case "html" => ctype = Some(MediaType.HTML)
        case "xhtml" => ctype = Some(MediaType.XHTML)
        case "text" => ctype = Some(MediaType.TEXT)
        case _ => ()
      }
    }

    item match {
      case node: XdmNode =>
        node.getNodeKind match {
          case XdmNodeKind.DOCUMENT =>
            var textOnly = true
            for (child <- S9Api.axis(node, Axis.CHILD)) {
              textOnly = textOnly && child.getNodeKind == XdmNodeKind.TEXT
            }
            if (ctype.isEmpty) {
              ctype = if (textOnly) {
                Some(MediaType.TEXT)
              } else {
                Some(MediaType.XML)
              }
            }
          case XdmNodeKind.TEXT =>
            if (ctype.isEmpty) {
              ctype = Some(MediaType.TEXT)
            }
          case _ =>
            if (ctype.isEmpty) {
              ctype = Some(MediaType.XML)
            }
        }

        if (node.getNodeKind != XdmNodeKind.DOCUMENT) {
          val builder = new SaxonTreeBuilder(config)
          builder.startDocument(node.getBaseURI)
          builder.addSubtree(node)
          builder.endDocument()
          outputItem = builder.result
        }

      case _: XdmAtomicValue =>
        ctype = Some(MediaType.JSON)

      case _: XdmArray =>
        ctype = Some(MediaType.JSON)

      case _: XdmMap =>
        ctype = Some(MediaType.JSON)

      case _ =>
        throw new RuntimeException("Unexpected item type produced by XSLT: " + item)
    }

    val mtype = new XProcMetadata(ctype, dprop.toMap)
    consumer.get.receive(port, outputItem, mtype)
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

      val dest = if (tree.getOrElse("yes") == "yes") {
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

  class MyResultDocumentResolver(val sconfig: Configuration) extends ResultDocumentResolver() {
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

      val pc = new PipelineConfiguration(sconfig)
      destination.getReceiver(pc, properties);
    }
  }

  private class CatchMessages extends MessageListener {
    override def message(content: XdmNode, terminate: Boolean, locator: SourceLocator): Unit = {
      val treeWriter = new SaxonTreeBuilder(config)
      treeWriter.startDocument(content.getBaseURI)
      treeWriter.addStartElement(XProcConstants.c_error)
      treeWriter.addSubtree(content)
      treeWriter.addEndElement()
      treeWriter.endDocument()

      // FIXME: step.reportError(treeWriter.getResult());
      // FIXME: step.info(step.getNode(), content.toString());
    }
  }

  private class MyErrorListener(val compileTime: Boolean) extends ErrorListener {
    override def warning(e: TransformerException): Unit = ()

    override def error(e: TransformerException): Unit = {
      goesBang = Some(XProcException.xcXsltCompileError(e.getMessage, location))
    }

    override def fatalError(e: TransformerException): Unit = {
      goesBang = Some(XProcException.xcXsltCompileError(e.getMessage, location))
    }
  }
}
