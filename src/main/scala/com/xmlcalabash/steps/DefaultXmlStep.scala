package com.xmlcalabash.steps

import com.jafpl.graph.Location
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ImplParams, StaticContext, XMLCalabashRuntime, XProcDataConsumer, XProcMetadata, XmlPortSpecification, XmlStep}
import com.xmlcalabash.util.XProcVarValue
import net.sf.saxon.s9api.{QName, Serializer, XdmAtomicValue, XdmMap, XdmValue}
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
  protected val bindings = mutable.HashMap.empty[QName,XProcVarValue]

  def location: Option[Location] = _location

  // ==========================================================================

  override def setLocation(location: Location): Unit = {
    _location = Some(location)
  }
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(variable: QName, value: XdmValue, context: StaticContext): Unit = {
    bindings.put(variable, new XProcVarValue(value, context))
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

  def stringBinding(name: QName): Option[String] = {
    if (bindings.contains(name)) {
      Some(bindings(name).getStringValue)
    } else {
      None
    }
  }

  def booleanBinding(name: QName): Option[Boolean] = {
    if (bindings.contains(name)) {
      Some(bindings(name).getStringValue == "true")
    } else {
      None
    }
  }

  def integerBinding(name: QName): Option[Integer] = {
    if (bindings.contains(name)) {
      Some(bindings(name).getStringValue.toInt)
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


    for ((qname,property) <- stringMapping) {
      if (options.string(qname).isDefined) {
        serializer.setOutputProperty(property, options.string(qname).get)
      }
    }

    for ((qname,property) <- booleanMapping) {
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

    // FIXME: serialization map should be automatically converted to qnames
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
