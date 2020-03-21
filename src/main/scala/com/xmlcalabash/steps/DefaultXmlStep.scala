package com.xmlcalabash.steps

import java.io.{InputStream, OutputStream}
import java.net.URI

import com.jafpl.graph.Location
import com.jafpl.messages.Message
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime._
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api._
import net.sf.saxon.value.QNameValue
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.collection.mutable

class DefaultXmlStep extends XmlStep {
  private val stringMapping = Map(
    XProcConstants._method -> Serializer.Property.METHOD,
    XProcConstants._version -> Serializer.Property.VERSION,
    XProcConstants._doctype_public -> Serializer.Property.DOCTYPE_PUBLIC,
    XProcConstants._doctype_system -> Serializer.Property.DOCTYPE_SYSTEM,
    XProcConstants._media_type -> Serializer.Property.MEDIA_TYPE,
    XProcConstants._normalization_form -> Serializer.Property.NORMALIZATION_FORM,
    XProcConstants._item_separator -> Serializer.Property.ITEM_SEPARATOR,
    XProcConstants._html_version -> Serializer.Property.HTML_VERSION,
    XProcConstants.sx_indent_spaces -> Serializer.Property.SAXON_INDENT_SPACES,
    XProcConstants.sx_line_length -> Serializer.Property.SAXON_LINE_LENGTH,
    XProcConstants.sx_attribute_order -> Serializer.Property.SAXON_ATTRIBUTE_ORDER,
    XProcConstants.sx_newline -> Serializer.Property.SAXON_NEWLINE,
    XProcConstants.sx_suppress_indentation -> Serializer.Property.SAXON_SUPPRESS_INDENTATION,
    XProcConstants.sx_double_space -> Serializer.Property.SAXON_DOUBLE_SPACE,
    XProcConstants.sx_character_representation -> Serializer.Property.SAXON_CHARACTER_REPRESENTATION
  )

  private val booleanMapping = Map(
    XProcConstants._indent -> Serializer.Property.INDENT,
    XProcConstants._byte_order_mark -> Serializer.Property.BYTE_ORDER_MARK,
    XProcConstants._escape_uri_attributes -> Serializer.Property.ESCAPE_URI_ATTRIBUTES,
    XProcConstants._include_content_type -> Serializer.Property.INCLUDE_CONTENT_TYPE,
    XProcConstants._omit_xml_declaration -> Serializer.Property.OMIT_XML_DECLARATION,
    XProcConstants._undeclare_prefixes -> Serializer.Property.UNDECLARE_PREFIXES,
    XProcConstants.sx_canonical -> Serializer.Property.SAXON_CANONICAL,
    XProcConstants.sx_recognize_binary -> Serializer.Property.SAXON_RECOGNIZE_BINARY
  )

  private var _location = Option.empty[Location]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[XProcDataConsumer] = None
  protected var config: XMLCalabashRuntime = _
  protected val bindingContexts = mutable.HashMap.empty[QName, StaticContext]
  protected val bindings = mutable.HashMap.empty[QName, XdmValue]

  def location: Option[Location] = _location

  // ==========================================================================

  override def setLocation(location: Location): Unit = {
    _location = Some(location)
  }

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.NONE

  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(variable: QName, value: XdmValue, context: StaticContext): Unit = {
    bindingContexts.put(variable, context)
    bindings.put(variable, value)
  }

  override def setConsumer(consumer: XProcDataConsumer): Unit = {
    this.consumer = Some(consumer)
  }

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    // nop
  }

  override def configure(config: XMLCalabashConfig, params: Option[ImplParams]): Unit = {
    // nop
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    config match {
      case xmlCalabash: XMLCalabashRuntime =>
        this.config = xmlCalabash
      case _ => throw XProcException.xiNotXMLCalabash()
    }
  }

  override def run(context: StaticContext): Unit = {
    if (_location.isEmpty) {
      _location = context.location
    }
  }

  override def reset(): Unit = {
    // nop
  }

  override def abort(): Unit = {
    // nop
  }

  override def stop(): Unit = {
    // nop
  }

  def checkMetadata(result: Any, metadata: XProcMetadata): XProcMetadata = {
    val textOnly = result match {
      case node: XdmNode =>
        var textOnly = true
        node.getNodeKind match {
          case XdmNodeKind.DOCUMENT =>
            var count = 0
            val iter = node.axisIterator(Axis.CHILD)
            while (iter.hasNext) {
              val next = iter.next()
              count += 1
              textOnly = textOnly && next.getNodeKind == XdmNodeKind.TEXT
            }
            textOnly = textOnly && count == 1
            textOnly
          case _ => false
        }
      case _ => false
    }

    if (textOnly) {
      val props = mutable.HashMap.empty[QName, XdmValue]
      for ((name, value) <- metadata.properties) {
        name match {
          case XProcConstants._serialization => Unit
          case XProcConstants._content_type =>
            props.put(name, new XdmAtomicValue("text/plain"))
          case _ => props.put(name, value)
        }
      }
      new XProcMetadata(MediaType.TEXT, props.toMap)
    } else {
      metadata
    }
  }

  def definedBinding(name: QName): Boolean = {
    if (bindings.contains(name)) {
      val value = bindings(name).getUnderlyingValue
      value.getLength > 0
    } else {
      false
    }
  }

  def optionalStringBinding(name: QName): Option[String] = {
    if (definedBinding(name)) {
      Some(bindings(name).getUnderlyingValue.getStringValue)
    } else {
      None
    }
  }

  def stringBinding(name: QName): String = {
    stringBinding(name, "")
  }

  def stringBinding(name: QName, default: String): String = {
    if (definedBinding(name)) {
      bindings(name).getUnderlyingValue.getStringValue
    } else {
      default
    }
  }

  def optionalURIBinding(name: QName): Option[URI] = {
    if (definedBinding(name)) {
      val context = bindingContexts(name)
      Some(context.baseURI.get.resolve(bindings(name).getUnderlyingValue.getStringValue))
    } else {
      None
    }
  }

  def booleanBinding(name: QName): Option[Boolean] = {
    if (definedBinding(name)) {
      Some(bindings(name).getUnderlyingValue.getStringValue == "true")
    } else {
      None
    }
  }

  def integerBinding(name: QName): Option[Integer] = {
    if (definedBinding(name)) {
      Some(bindings(name).getUnderlyingValue.getStringValue.toInt)
    } else {
      None
    }
  }

  def mapBinding(name: QName): XdmMap = {
    if (definedBinding(name)) {
      val map = bindings(name)
      if (map.size > 0) {
        map.asInstanceOf[XdmMap]
      } else {
        new XdmMap()
      }
    } else {
      new XdmMap()
    }
  }

  def qnameBinding(name: QName): Option[QName] = {
    // This method doesn't distinguish between there was no binding for 'name' and
    // the binding for 'name' was not of type QName.
    if (definedBinding(name)) {
      bindings(name).getUnderlyingValue match {
        case qn: QNameValue =>
          Some(new QName(qn.getPrefix, qn.getNamespaceURI, qn.getLocalName))
        case _ => None
      }
    } else {
      None
    }
  }

  def serializationOptions(): Map[QName, String] = {
    serializationOptions(XProcConstants._serialization)
  }

  def serializationOptions(name: QName): Map[QName, String] = {
    val serialOpts = mutable.HashMap.empty[QName, String]
    val map = mapBinding(name)
    val iter = map.keySet.iterator()
    while (iter.hasNext) {
      val key = iter.next()
      serialOpts.put(key.getQNameValue, map.get(key).toString)
    }
    serialOpts.toMap
  }

  def serializationOption(name: QName): Option[String] = {
    val qn = new XdmAtomicValue(name)
    val smap = mapBinding(XProcConstants._serialization)
    if (smap.containsKey(qn)) {
      Some(smap.get(qn).getUnderlyingValue.getStringValue)
    } else {
      None
    }
  }

  def makeSerializer(optsmap: XdmMap): Serializer = {
    val serializer = config.processor.newSerializer()
    val options = new SerializationOptions(optsmap)

    // FIXME: Support with USE_CHARACTER_MAPS
    // FIXME: Support with CDATA_SECTION_ELEMENTS

    val encoding = if (options.contains(XProcConstants._encoding)) {
      options.string(XProcConstants._encoding).get
    } else {
      "utf-8"
    }
    serializer.setOutputProperty(Serializer.Property.ENCODING, encoding)


    for ((qname, property) <- stringMapping) {
      if (options.string(qname).isDefined) {
        serializer.setOutputProperty(property, options.string(qname).get)
      }
    }

    for ((qname, property) <- booleanMapping) {
      if (options.string(qname).isDefined) {
        serializer.setOutputProperty(property, options.boolean(qname).get)
      }
    }

    val standalone = options.string(XProcConstants._standalone)
    if (standalone.isDefined) {
      standalone.get match {
        case "true" => serializer.setOutputProperty(Serializer.Property.STANDALONE, "yes")
        case "false" => serializer.setOutputProperty(Serializer.Property.STANDALONE, "no")
        case "omit" => serializer.setOutputProperty(Serializer.Property.STANDALONE, "omit")
        // Just in case...
        case "yes" => serializer.setOutputProperty(Serializer.Property.STANDALONE, "yes")
        case "no" => serializer.setOutputProperty(Serializer.Property.STANDALONE, "no")
      }
    }

    serializer
  }

  def documentIsText(node: XdmNode): Boolean = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        var text = true
        for (child <- S9Api.axis(node, Axis.CHILD)) {
          text = text && child.getNodeKind == XdmNodeKind.TEXT
        }
        text
      case XdmNodeKind.TEXT =>
        true
      case _ =>
        false
    }
  }

  def convertMetadataToText(meta: XProcMetadata): XProcMetadata = {
    val props = mutable.HashMap.empty[QName, XdmValue]
    for ((key, value) <- meta.properties) {
      key match {
        case XProcConstants._serialization => Unit
        case XProcConstants._content_type =>
          props(key) = new XdmAtomicValue("text/plain")
        case _ =>
          props(key) = value
      }
    }

    new XProcMetadata(MediaType.TEXT, props.toMap)
  }

  def serialize(context: StaticContext, source: Any, metadata: XProcMetadata, output: OutputStream): Unit = {
    source match {
      case bn: BinaryNode =>
        val bytes = new Array[Byte](8192)
        val stream = bn.stream
        var count = stream.read(bytes)
        while (count >= 0) {
          output.write(bytes, 0, count)
          count = stream.read(bytes)
        }
      case is: InputStream =>
        val bytes = new Array[Byte](8192)
        var count = is.read(bytes)
        while (count >= 0) {
          output.write(bytes, 0, count)
          count = is.read(bytes)
        }
      case node: XdmNode =>
        val serialOpts = serializationOptions()
        val serializer = config.processor.newSerializer(output)

        val contentType = metadata.contentType
        if (!contentType.xmlContentType && !contentType.htmlContentType) {
          serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
        }

        S9Api.configureSerializer(serializer, config.defaultSerializationOptions(contentType))
        S9Api.configureSerializer(serializer, serialOpts)

        S9Api.serialize(config.config, node, serializer)
      case _: XdmMap =>
        serializeJson(context, source, metadata, output)
      case _: XdmArray =>
        serializeJson(context, source, metadata, output)
      case _ =>
        throw XProcException.xiUnexpectedItem(source.toString, context.location)
    }
  }

  private def serializeJson(context: StaticContext, source: Any, metadata: XProcMetadata, output: OutputStream): Unit = {
    val expr = new XProcXPathExpression(context, "serialize($map, map {\"method\": \"json\"})")
    val bindingsMap = mutable.HashMap.empty[String, Message]
    val vmsg = new XdmValueItemMessage(source.asInstanceOf[XdmValue], XProcMetadata.TEXT, context)
    bindingsMap.put("{}map", vmsg)
    val smsg = config.expressionEvaluator.singletonValue(expr, List(), bindingsMap.toMap, None)
    output.write(smsg.item.toString.getBytes())
  }

  def resolveURI(baseURI: URI, relative: String): URI = {
    try {
      baseURI.resolve(relative)
    } catch {
      case _: Exception =>
        throw XProcException.xdInvalidURI(relative, location)
    }
  }

  override def toString: String = {
    val defStr = super.toString
    if (defStr.startsWith("XXX com.xmlcalabash.steps")) {
      val objstr = ".*\\.([^\\.]+)@[0-9a-f]+$".r
      defStr match {
        case objstr(name) => name
        case _ => defStr

      }
    } else {
      defStr
    }
  }

  class SerializationOptions(map: XdmMap) {
    private val options = mutable.HashMap.empty[QName,XdmValue]

    for (entry <- map.entrySet().asScala) {
      val qname = entry.getKey.getQNameValue
      options.put(qname, entry.getValue)
    }

    def contains(key: QName): Boolean = {
      options.contains(key)
    }

    def string(key: QName): Option[String] = {
      if (options.contains(key)) {
        Some(options(key).getUnderlyingValue.getStringValue)
      } else {
        None
      }
    }

    def boolean(key: QName): Option[String] = {
      if (options.contains(key)) {
        val value = options(key).getUnderlyingValue.getStringValue
        if (value == "true") {
          Some("yes")
        } else {
          Some("no")
        }
      } else {
        None
      }
    }

  }
}
