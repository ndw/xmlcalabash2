package com.xmlcalabash.steps

import java.io.{IOException, StringReader}

import com.jafpl.steps.PortCardinality
import com.thaiopensource.util.PropertyMapBuilder
import com.thaiopensource.validate.auto.AutoSchemaReader
import com.thaiopensource.validate.prop.rng.RngProperty
import com.thaiopensource.validate.rng.CompactSchemaReader
import com.thaiopensource.validate.{SchemaReader, ValidateProperty, ValidationDriver}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.xc.Errors
import com.xmlcalabash.util.{CachingErrorListener, S9Api}
import net.sf.saxon.s9api.{QName, XdmNode}
import org.xml.sax.InputSource

import scala.xml.{SAXException, SAXParseException}

class ValidateWithRNG() extends DefaultXmlStep {
  private val _assert_valid = new QName("", "assert-valid")
  private val _dtd_attribute_values = new QName("", "dtd-attribute-values")
  private val _dtd_id_idref_warnings = new QName("", "dtd-id-idref-warnings")
  private val language = "http://relaxng.org/ns/structure/1.0"

  private var source: XdmNode = _
  private var sourceMetadata: XProcMetadata = _
  private var schema: XdmNode = _
  private var schemaMetadata: XProcMetadata = _
  private var assert_valid = true
  private var dtd_attribute_values = false
  private var dtd_id_idref_warnings = false

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.EXACTLY_ONE,
        "schema" -> PortCardinality.EXACTLY_ONE),
    Map("source" -> List("application/xml", "text/xml", "*/*+xml"),
        "schema" -> List("application/xml", "text/xml", "*/*+xml", "text/plain")))

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode =>
        port match {
          case "source" =>
            source = node
            sourceMetadata = metadata
          case "schema" =>
            schema = node
            schemaMetadata = metadata
          case _ => logger.debug(s"Unexpected connection to p:validate-with-xsd: $port")
        }
      case _ => throw new RuntimeException("Non-XML document passed to xsd validator?")
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    if (definedBinding(_dtd_id_idref_warnings)) {
      dtd_id_idref_warnings = booleanBinding(_dtd_id_idref_warnings).getOrElse(false)
    }

    val errors = new Errors(config.config)
    val listener = new CachingErrorListener(errors)
    val properties = new PropertyMapBuilder()
    properties.put(ValidateProperty.ERROR_HANDLER, listener)
    properties.put(ValidateProperty.URI_RESOLVER, config.uriResolver)
    properties.put(ValidateProperty.ENTITY_RESOLVER, config.entityResolver)

    if (dtd_id_idref_warnings) {
      RngProperty.CHECK_ID_IDREF.add(properties)
    }

    val docBaseURI = source.getBaseURI

    val compact = schemaMetadata.contentType.textContentType

    val configurer = config.xprocConfigurer.jingConfigurer
    var sr: SchemaReader = null
    var schemaInputSource: InputSource = null

    if (compact) {
      configurer.configRNC(properties)
      sr = CompactSchemaReader.getInstance()

      // Grotesque hack!
      val srdr = new StringReader(schema.getStringValue)
      schemaInputSource = new InputSource(srdr)
      schemaInputSource.setSystemId(schema.getBaseURI.toASCIIString)
    } else {
      configurer.configRNG(properties)
      sr = new AutoSchemaReader()
      schemaInputSource = S9Api.xdmToInputSource(config.config, schema)
    }

    val driver = new ValidationDriver(properties.toPropertyMap, sr)

    try {
      if (driver.loadSchema(schemaInputSource)) {
        val din = S9Api.xdmToInputSource(config.config, source)
        if (!driver.validate(din)) {
          if (assert_valid) {
            var msg = "RELAX NG validation failed"
            if (listener.exceptions.nonEmpty) {
              val lex = listener.exceptions.head
              lex match {
                case _: Exception =>
                  msg = lex.getMessage
                  val except = XProcException.xcNotSchemaValid(source.getBaseURI.toASCIIString, msg, location)
                  except.underlyingCauses = listener.exceptions
                  throw except
              }
            } else {
              val except = XProcException.xcNotSchemaValid(source.getBaseURI.toASCIIString, msg, location)
              except.underlyingCauses = listener.exceptions
              throw except
            }
          }
        }
      } else {
        throw XProcException.xcNotSchemaValid(source.getBaseURI.toASCIIString, "Error loading schema", location)
      }
    } catch {
      case ex: SAXParseException =>
        throw new RuntimeException("SAX Parse Exception")
      case ex: SAXException =>
        throw new RuntimeException("SAX Exception")
      case ex: IOException =>
        throw new RuntimeException("IO Exception")
    }

    consumer.get.receive("result", source, sourceMetadata)
  }
}
