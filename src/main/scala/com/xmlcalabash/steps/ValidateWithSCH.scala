package com.xmlcalabash.steps

import java.io.InputStream

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api}
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.{Source, URIResolver}
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmDestination, XdmNode}
import org.xml.sax.InputSource

class ValidateWithSCH() extends DefaultXmlStep {
  private val _untyped = StructuredQName.fromClarkName("{http://www.w3.org/2001/XMLSchema}untyped")
  private val _assert_valid = new QName("", "assert-valid")
  private val _phase = new QName("", "phase")

  private var source: XdmNode = _
  private var sourceMetadata: XProcMetadata = _
  private var schema: XdmNode = _
  private var assert_valid = true
  private var phase = Option.empty[String]
  private var schemaAware = false
  private var skeleton: InputStream = _

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.EXACTLY_ONE,
        "schema" -> PortCardinality.EXACTLY_ONE),
    Map("source" -> List("application/xml", "text/xml", "*/*+xml"),
        "schema" -> List("application/xml", "text/xml", "*/*+xml")))

  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.EXACTLY_ONE,
        "report" -> PortCardinality.ZERO_OR_MORE),
    Map("result" -> List("application/xml"),
        "report" -> List("application/xml"))
  )

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode =>
        port match {
          case "source" =>
            source = node
            sourceMetadata = metadata
          case "schema" => schema = node
          case _ => logger.debug(s"Unexpected connection to p:validate-with-schematron: $port")
        }
      case _ => throw new RuntimeException("Non-XML document passed to XML Schematron validator?")
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val stype = source.getUnderlyingNode.getSchemaType
    schemaAware = ! (stype == null || stype.getStructuredQName.equals(_untyped))

    // From http://www.schematron.com/
    // ...
    // So the basic processing now looks like this:
    //
    // xslt -stylesheet iso_dsdl_include.xsl  theSchema.sch > theSchema1.sch
    // xslt -stylesheet iso_abstract_expand.xsl  theSchema1.sch > theSchema2.sch
    // xslt -stylesheet iso_svrl_for_xsltn.xsl  theSchema2.sch > theSchema.xsl
    // xslt -stylesheet theSchema.xsl  myDocument.xml > myResult.xml

    // It would be nice to load these stylesheets only once, but sometimes (i.e. from RunTest),
    // there are different controllers involved and you can't do that.
    val theSchema1_sch = transform(schema, getSchematronXSLT("iso_dsdl_include.xsl"))
    val theSchema2_sch = transform(theSchema1_sch, getSchematronXSLT("iso_abstract_expand.xsl"))

    skeleton = getClass.getResourceAsStream("/etc/schematron/iso_schematron_skeleton_for_saxon.xsl")
    if (skeleton == null) {
      throw new RuntimeException(s"Failed to load skeleton")
    }

    var compiler = config.processor.newXsltCompiler()
    compiler.setSchemaAware(schemaAware)
    compiler.setURIResolver(new UResolver)
    var exec = compiler.compile(getSchematronXSLT("iso_svrl_for_xslt2.xsl"))
    val schemaCompiler = exec.load()

    if (phase.isDefined) {
      schemaCompiler.setParameter(_phase, new XdmAtomicValue(phase.get))
    }

    // FIXME: handle parameters

    schemaCompiler.setInitialContextNode(theSchema2_sch)
    var result = new XdmDestination
    schemaCompiler.setDestination(result)

    config.xprocConfigurer.saxonConfigurer.configureSchematron(schemaCompiler.getUnderlyingController.getConfiguration)

    schemaCompiler.transform()

    val compiledSchema = result.getXdmNode
    val compiledRoot = S9Api.documentElement(compiledSchema)

    if (compiledRoot.isEmpty) {
      val schemaRootNode = S9Api.documentElement(schema)
      val root = if (schemaRootNode.isDefined) {
        schemaRootNode.get.getNodeKind.toString
      } else {
        "null"
      }
      throw new RuntimeException(s"Failed to compile provided schema $root")
    }

    compiler = config.processor.newXsltCompiler()
    compiler.setSchemaAware(schemaAware)
    exec = compiler.compile(compiledSchema.asSource())
    val transformer = exec.load()

    // FIXME: handle parameters here too

    transformer.setInitialContextNode(source)
    result = new XdmDestination
    transformer.setDestination(result)
    transformer.transform()

    val report = result.getXdmNode

    consumer.get.receive("report", report, new XProcMetadata(MediaType.XML))
    consumer.get.receive("result", source, sourceMetadata)
  }

  private def getSchematronXSLT(xslt: String): SAXSource = {
    val instream = getClass.getResourceAsStream(s"/etc/schematron/$xslt")
    if (instream == null) {
      throw new RuntimeException(s"Failed to load Schematron XSLT: /etc/schematron/$xslt from jar")
    }
    new SAXSource(new InputSource(instream))
  }

  private def transform(source: XdmNode, stylesheet: SAXSource): XdmNode = {
    val compiler = config.processor.newXsltCompiler()
    compiler.setSchemaAware(schemaAware)
    compiler.setURIResolver(new UResolver())
    val exec = compiler.compile(stylesheet)
    val schemaCompiler = exec.load()

    schemaCompiler.setInitialContextNode(source)
    val result = new XdmDestination()
    schemaCompiler.setDestination(result)
    schemaCompiler.transform()
    result.getXdmNode
  }

  private class UResolver extends URIResolver {
    override def resolve(href: String, base: String): Source = {
      if (href == "iso_schematron_skeleton_for_saxon.xsl") {
        new SAXSource(new InputSource(skeleton))
      } else {
        throw new RuntimeException(s"Failed to resolve $href from jar")
      }
    }
  }
}
