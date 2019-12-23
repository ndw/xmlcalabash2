package com.xmlcalabash.steps

import java.io.ByteArrayOutputStream
import java.net.URI

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, XProcCollectionFinder}
import javax.xml.transform.{ErrorListener, TransformerException}
import net.sf.saxon.event.{PipelineConfiguration, Receiver}
import net.sf.saxon.s9api.{QName, Serializer, ValidationMode, XdmAtomicValue, XdmDestination, XdmItem, XdmNode, XdmNodeKind, XdmValue}
import net.sf.saxon.serialize.SerializationProperties

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XQuery extends DefaultXmlStep {
  private var query = Option.empty[XdmNode]
  private var queryMetadata = Option.empty[XProcMetadata]
  private val defaultCollection = ListBuffer.empty[XdmNode]
  private val sources = ListBuffer.empty[XdmItem]

  private var parameters = Map.empty[QName, XdmValue]
  private var version = Option.empty[String]

  private var goesBang = Option.empty[XProcException]
  private val outputProperties = mutable.HashMap.empty[QName, XdmValue]

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.ZERO_OR_MORE, "query" -> PortCardinality.EXACTLY_ONE),
    Map("source" -> List("application/octet-stream"), "query" -> List("application/xml", "text/plain"))
  )

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    port match {
      case "source" =>
        item match {
          case node: XdmNode =>
            defaultCollection += node
            sources += node
          case item: XdmItem =>
            sources += item
        }
      case "query" =>
        query = Some(item.asInstanceOf[XdmNode])
        queryMetadata = Some(metadata)
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
    super.run(staticContext)
    val document: Option[XdmItem] = sources.headOption

    if (version.isDefined && version.get != "3.0" && version.get != "3.1") {
      throw XProcException.xcXQueryVersionNotAvailable(version.getOrElse(""), location)
    }

    val runtime = this.config.config
    val processor = runtime.processor
    val underlyingConfig = processor.getUnderlyingConfiguration
    // FIXME: runtime.getConfigurer().getSaxonConfigurer().configXQuery(config);

    val collectionFinder = underlyingConfig.getCollectionFinder
    val unparsedTextURIResolver = underlyingConfig.getUnparsedTextURIResolver

    underlyingConfig.setDefaultCollection(XProcCollectionFinder.DEFAULT)
    underlyingConfig.setCollectionFinder(new XProcCollectionFinder(runtime, defaultCollection.toList, collectionFinder))

    val compiler = processor.newXQueryCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)
    compiler.setErrorListener(new MyErrorListener(true))
    val exec = try {
      var xquery = query.get.getStringValue
      if (queryMetadata.get.contentType.xmlContentType) {
        val root = S9Api.documentElement(query.get)
        if (root.get.getNodeName != XProcConstants.c_query) {
          val baos = new ByteArrayOutputStream()
          val serializer = config.processor.newSerializer(baos)
          serializer.setOutputProperty(Serializer.Property.ENCODING, "utf-8")
          serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "true")
          S9Api.serialize(config.config, List(query.get), serializer)
          xquery = baos.toString("utf-8")
        }
      }

      compiler.compile(xquery)
    } catch {
      case e: Exception =>
        throw goesBang.getOrElse(e)
    }
    val queryEval = exec.load()

    for ((param, value) <- parameters) {
      queryEval.setExternalVariable(param, value)
    }

    if (document.isDefined) {
      queryEval.setContextItem(document.get)
    }

    val result = new MyDestination(new XdmDestination(), outputProperties)
    queryEval.setDestination(result)

    queryEval.setSchemaValidationMode(ValidationMode.DEFAULT)
    queryEval.setErrorListener(new MyErrorListener(false))
    // FIXME: transformer.getUnderlyingController().setUnparsedTextURIResolver(unparsedTextURIResolver)

    try {
      val iter = queryEval.iterator()
      while (iter.hasNext) {
        val item = iter.next()
        if (item.isAtomicValue) {
          consumer.get.receive("result", item, new XProcMetadata(MediaType.JSON))
        } else {
          val node = item.asInstanceOf[XdmNode]
          val builder = new SaxonTreeBuilder(config)
          builder.startDocument(None)
          builder.addSubtree(node)
          builder.endDocument()

          if (node.getNodeKind == XdmNodeKind.TEXT) {
            consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.TEXT))
          } else {
            consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
          }
        }
      }
    } catch {
      case e: Exception =>
        throw goesBang.getOrElse(e)
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

  private class MyErrorListener(val compileTime: Boolean) extends ErrorListener {
    override def warning(e: TransformerException): Unit = Unit

    override def error(e: TransformerException): Unit = {
      goesBang = Some(XProcException.xcXsltCompileError(e.getMessage, location))
    }

    override def fatalError(e: TransformerException): Unit = {
      if (compileTime) {
        goesBang = Some(XProcException.xcXQueryCompileError(e.getMessage, location))
      } else {
        goesBang = Some(XProcException.xcXQueryEvalError(e.getMessage, location))
      }
    }
  }
}
