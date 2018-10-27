package com.xmlcalabash.steps

import java.io.IOException

import com.jafpl.steps.PortCardinality
import com.sun.msv.verifier.ValidityViolation
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{CachingErrorListener, S9Api}
import net.sf.saxon.s9api.{QName, XdmNode}
import org.iso_relax.verifier.VerifierConfigurationException

import scala.xml.SAXException

class ValidateWithRNG() extends DefaultXmlStep {
  private val _assert_valid = new QName("", "assert-valid")
  private val _dtd_attribute_values = new QName("", "dtd-attribute-values")
  private val _dtd_id_idref_warnings = new QName("", "dtd-id-idref-warnings")
  private val language = "http://relaxng.org/ns/structure/1.0"

  private var source: XdmNode = _
  private var sourceMetadata: XProcMetadata = _
  private var schema: XdmNode = _
  private var assert_valid = true
  private var dtd_compatibility = false

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.EXACTLY_ONE,
        "schema" -> PortCardinality.EXACTLY_ONE),
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
          case "schema" => schema = node
          case _ => logger.debug(s"Unexpected connection to p:validate-with-xsd: $port")
        }
      case _ => throw new RuntimeException("Non-XML document passed to xsd validator?")
    }
  }

  override def run(context: StaticContext): Unit = {
    try {
      val vfactory = new com.sun.msv.verifier.jarv.TheFactoryImpl()
      val schemaSource = S9Api.xdmToInputSource(config.config, schema)
      schemaSource.setSystemId(schema.getBaseURI.toASCIIString)

      val listener = new CachingErrorListener()
      val docSchema = vfactory.compileSchema(schemaSource)
      val verifier = docSchema.newVerifier()
      verifier.setErrorHandler(listener)

      if (!verifier.verify(S9Api.xdmToInputSource(config.config, source))) {
        var msg = "RELAX NG validation failed"
        if (listener.exceptions.nonEmpty) {
          val lex = listener.exceptions.head
          lex match {
            case ex: ValidityViolation =>
              msg = ex.getMessage
              val except = XProcException.xcNotSchemaValid(source.getBaseURI.toASCIIString, ex.getLineNumber, ex.getColumnNumber, msg, location)
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
      }

      consumer.get.receive("result", source, sourceMetadata)
    } catch {
      case ex: VerifierConfigurationException =>
        throw new RuntimeException("VCD")
      case ex: SAXException =>
        throw new RuntimeException("SAX")
      case ex: IOException =>
        throw new RuntimeException("SAX")
    }
  }
}
