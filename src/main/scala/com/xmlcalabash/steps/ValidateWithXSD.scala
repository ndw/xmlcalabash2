package com.xmlcalabash.steps

import java.lang.reflect.InvocationTargetException

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{CachingErrorListener, MediaType, S9Api}
import net.sf.saxon.Controller
import net.sf.saxon.`type`.ValidationException
import net.sf.saxon.s9api.{QName, SaxonApiException, SchemaManager, XdmDestination, XdmNode}
import net.sf.saxon.serialize.SerializationProperties

import scala.collection.mutable.ListBuffer

class ValidateWithXSD() extends DefaultXmlStep {
  private val _use_location_hints = new QName("","use-location-hints")
  private val _try_namespaces = new QName("", "try-namespaces")
  private val _assert_valid = new QName("", "assert-valid")
  private val _mode = new QName("", "mode")
  private val _version = new QName("", "version")

  private var source: XdmNode = _
  private var sourceMetadata: XProcMetadata = _
  private val schemas = ListBuffer.empty[XdmNode]
  private var use_location_hints = false
  private var try_namespaces = false
  private var assert_valid = true
  private var mode = "strict"
  private var version = "1.1"

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.ONE_OR_MORE,
        "schema" -> PortCardinality.ZERO_OR_MORE),
    Map("source" -> List("application/xml", "text/xml", "*/*+xml"),
        "schema" -> List("application/xml", "text/xml", "*/*+xml")))

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode =>
        port match {
          case "source" =>
            source = node
            sourceMetadata = metadata
          case "schema" => schemas += node
          case _ => logger.debug(s"Unexpected connection to p:validate-with-xsd: $port")
        }
      case _ => throw new RuntimeException("Non-XML document passed to xsd validator?")
    }
  }

  override def run(context: StaticContext): Unit = {
    val manager = Option(config.processor.getSchemaManager)
    if (manager.isDefined) {
      validateWithSaxon(manager.get)
    } else {
      throw new RuntimeException("Validation requires Saxon EE")
    }
  }

  private def validateWithSaxon(manager: SchemaManager): Unit = {
    logger.trace(s"Validating with Saxon: ${source.getBaseURI} with ${schemas.length} schema(s)")

    version = stringBinding(_version, version)

    val saxonConfig = config.processor.getUnderlyingConfiguration

    // Saxon 9.2.0.4j introduces a clearSchemaCache method on Configuration.
    // Call it if it's available.
    try {
      val clearSchemaCache = config.getClass.getMethod("clearSchemaCache", null)
      clearSchemaCache.invoke(config)
      logger.trace("Cleared schema cache.")
    } catch {
      case nsme: NoSuchMethodException =>
        // nop; oh, well
        logger.debug("Cannot reset schema cache: no such method")
      case nsme: IllegalAccessException =>
        logger.debug("Cannot reset schema cache: illegal access exception")
      case nsme: InvocationTargetException =>
        logger.debug("Cannot reset schema cache: invocation target exception")
    }

    if (bindings.contains(_try_namespaces)) {
      val namespace = S9Api.documentElement(source).get.getNodeName.getNamespaceURI
      try_namespaces = booleanBinding(_try_namespaces).getOrElse(false)
      try_namespaces = try_namespaces && namespace != ""
    }

    mode = stringBinding(_mode, mode)
    assert_valid = booleanBinding(_assert_valid).getOrElse(false)
    use_location_hints = booleanBinding(_use_location_hints).getOrElse(false)

    // FIXME: populate the URI cache so that computed schema documents will be found preferentially

    // FIXME: support try_namespaces

    for (schema <- schemas) {
      manager.load(schema.asSource())
    }

    val destination = new XdmDestination
    val controller = new Controller(saxonConfig)
    val pipe = controller.makePipelineConfiguration()
    val receiver = destination.getReceiver(pipe, new SerializationProperties())
    pipe.setRecoverFromValidationErrors(assert_valid)
    receiver.setPipelineConfiguration(pipe)

    val listener = new CachingErrorListener()
    val validator = manager.newSchemaValidator()
    validator.setDestination(destination)
    validator.setErrorListener(listener)
    validator.setLax(mode == "lax")
    validator.setUseXsiSchemaLocation(use_location_hints)

    try {
      validator.validate(source.asSource())
    } catch {
      case ex: SaxonApiException =>
        var msg = ex.getMessage
        if (listener.exceptions.nonEmpty) {
          val lex = listener.exceptions.head
          lex match {
            case ve: ValidationException =>
              msg = ve.getMessage
              val fail = ve.getValidationFailure
              val except = XProcException.xcNotSchemaValid(source.getBaseURI.toASCIIString, fail.getLineNumber, fail.getColumnNumber, msg, location)
              except.underlyingCauses = listener.exceptions
              throw except
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
      case ex: Exception =>
        throw XProcException.xcNotSchemaValid(source.getBaseURI.toASCIIString, ex.getMessage, location)
    }

    val metadata = new XProcMetadata(MediaType.XML)
    consumer.get.receive("result", destination.getXdmNode, sourceMetadata)
  }
}
