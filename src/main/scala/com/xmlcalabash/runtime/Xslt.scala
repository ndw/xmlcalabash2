package com.xmlcalabash.runtime

import java.net.URI
import javax.xml.transform.{Result, SourceLocator}

import com.jafpl.messages.ItemMessage
import com.xmlcalabash.util.TreeWriter
import com.xmlcalabash.core.{XProcConstants, XProcEngine, XProcException}
import com.xmlcalabash.items.{StringItem, XPathDataModelItem}
import com.xmlcalabash.util.NodeUtils
import net.sf.saxon.lib.OutputURIResolver
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.Sequence
import net.sf.saxon.s9api._
import net.sf.saxon.value.{AtomicValue, Int64Value, QNameValue, StringValue}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/3/16.
  */
class Xslt extends DefaultXProcStep {
  private val _initial_mode = new QName("", "initial-mode")
  private val _template_name = new QName("", "template-name")
  private val _output_base_uri = new QName("", "output-base-uri")
  private val _version = new QName("", "version")
  private val _content_type = new QName("content-type")
  private val _parameters = new QName("parameters")
  private val cx_decode = new QName("cx", XProcConstants.NS_CALABASH_EX, "decode")

  val defaultCollection = ListBuffer.empty[XdmValue]
  var stylesheet: Option[XdmNode] = None

  override def receive(port: String, msg: ItemMessage): Unit = {
    super.receive(port, msg)

    val value =
      msg.item match {
        case item: XPathDataModelItem => item.value
        case item: StringItem => getUntypedAtomic(engine.processor, item.get)
        case _ =>
          logger.warn("Unexpected input on port: " + port)
          throw new XProcException("Bad source value")
      }

    port match {
      case "source" => defaultCollection += value
      case "stylesheet" => stylesheet = Some(value.asInstanceOf[XdmNode])
      case _ =>
        if (port.startsWith("{")) {
          options.put(parseClarkName(port), value)
        } else {
          logger.warn("Unexpected port name: " + port)
        }
    }
  }

  override def reset(): Unit = {
    defaultCollection.clear()
    stylesheet = None
    options.clear()
  }

  override def run(): Unit = {
    super.run()

    val document = defaultCollection.headOption
    var version = getStringOption(_version)
    if (version.isEmpty && stylesheet.isDefined && stylesheet.get.isInstanceOf[XdmNode]) {
      val root = NodeUtils.getDocumentElement(stylesheet.get.asInstanceOf[XdmNode])
      version = Option(root.get.getAttributeValue(_version))
    }

    // FIXME: What to do about 1.0?

    val initialMode = getQNameOption(_initial_mode)
    val templateName = getQNameOption(_template_name)
    val outputBaseURI = getStringOption(_output_base_uri)
    val params = options.get(_parameters)

    val config = engine.processor.getUnderlyingConfiguration

    // FIXME: runtime.getConfigurer().getSaxonConfigurer().configXSLT(config);

    // FIXME: val uriResolver =
    // FIXME: val collectionResolver =
    // FIXME: val unparsedTextURIResolver =

    config.setOutputURIResolver(new OutputResolver())
    // FIXME: config.setCollectionFinder(...)

    val compiler = engine.processor.newXsltCompiler()
    compiler.setSchemaAware(engine.processor.isSchemaAware)
    val exec = compiler.compile(stylesheet.get.asSource())
    val transformer = exec.load()

    // FIXME: parameters...
    if (params.isDefined) {
      if (params.get.size() != 1) {
        logger.warn("Parameter map is a list!?")
      } else {
        val item = params.get.itemAt(0).getUnderlyingValue
        if (item.isInstanceOf[MapItem]) {
          val iter = item.asInstanceOf[MapItem].iterator()
          while (iter.hasNext) {
            val pair = iter.next
            val pkey = pair.key
            val value = XdmValue.wrap(pair.value)

            var name: Option[QName] = None

            pkey match {
              case qn: QNameValue =>
                name = Some(new QName(qn.getStructuredQName))
              case sv: StringValue =>
                val str = sv.getStringValue
                if (str.contains(":")) {
                  logger.warn("Lexical qnames aren't supported yet")
                } else {
                  name = Some(new QName("", str))
                }
              case _ =>
                logger.warn("Unexpected key type: " + pkey)
            }

            if (name.isDefined) {
              transformer.setParameter(name.get, value)
            }
          }
        }

      }
    }

    if (document.isDefined) {
      transformer.setInitialContextNode(document.get.asInstanceOf[XdmNode])
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
      controller.send("result", new XPathDataModelItem(xformed.get))
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

      val receiver = xdmResult.getReceiver(engine.processor.getUnderlyingConfiguration)
      receiver.setSystemId(baseURI.toASCIIString)
      receiver
    }

    def close(result: Result): Unit = {
      val href = result.getSystemId
      val xdmResult = secondaryResults(href)
      val doc = xdmResult.getXdmNode
      controller.send("secondary", new XPathDataModelItem(doc))
    }
  }

  class CatchMessages extends MessageListener {
    override def message(content: XdmNode, terminate: Boolean, locator: SourceLocator): Unit = {
      val treeWriter = new TreeWriter(engine)
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
