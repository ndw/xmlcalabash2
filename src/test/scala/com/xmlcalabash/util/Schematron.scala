package com.xmlcalabash.util

import java.net.URI
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.{Source, URIResolver}

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.TestException
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmDestination, XdmNode}
import org.xml.sax.InputSource

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Schematron(runtimeConfig: XMLCalabash) {
  private val _untyped = StructuredQName.fromClarkName("{http://www.w3.org/2001/XMLSchema}untyped")
  private val uResolver = new UResolver()

  def test(sourceXML: XdmNode, schemaXML: XdmNode): List[XdmNode] = {
    test(sourceXML, schemaXML, None)
  }

  def test(sourceXML: XdmNode, schemaXML: XdmNode, phase: Option[String]): List[XdmNode] = {
    val schemaType = sourceXML.getUnderlyingNode.getSchemaType
    val schemaAware = (schemaType != null) && (schemaType.getStructuredQName != _untyped)
    val params = mutable.HashMap.empty[QName, Any] // FIXME: Make user accessible

    // From http://www.schematron.com/
    // ...
    // So the basic processing now looks like this:
    //
    // xslt -stylesheet iso_dsdl_include.xsl  theSchema.sch > theSchema1.sch
    // xslt -stylesheet iso_abstract_expand.xsl  theSchema1.sch > theSchema2.sch
    // xslt -stylesheet iso_svrl_for_xsltn.xsl  theSchema2.sch > theSchema.xsl
    // xslt -stylesheet theSchema.xsl  myDocument.xml > myResult.xml

    val theSchema1_sch = transform(schemaXML, getSchematronXSLT("iso_dsdl_include.xsl"), schemaAware)
    val theSchema2_sch = transform(theSchema1_sch, getSchematronXSLT("iso_abstract_expand.xsl"), schemaAware)

    var compiler = runtimeConfig.processor.newXsltCompiler()
    compiler.setSchemaAware(schemaAware)
    compiler.setURIResolver(uResolver)
    var exec = compiler.compile(getSchematronXSLT("iso_svrl_for_xslt2.xsl"))
    val schemaCompiler = exec.load()

    if (phase.isDefined) {
      schemaCompiler.setParameter(new QName("", "phase"), new XdmAtomicValue(phase.get))
    }

    for ((name, value) <- params) {
      schemaCompiler.setParameter(name, new XdmAtomicValue(value.toString))
    }

    schemaCompiler.setInitialContextNode(theSchema2_sch)
    var result: XdmDestination = new XdmDestination()
    schemaCompiler.setDestination(result)

    schemaCompiler.transform()
    val compiledSchema = result.getXdmNode
    val compiledRoot = S9Api.documentElement(compiledSchema)
    if (compiledRoot.isEmpty) {
      throw new TestException("Failed to compile schema provided")
    }

    compiler = runtimeConfig.processor.newXsltCompiler()
    compiler.setSchemaAware(schemaAware)
    exec = compiler.compile(compiledSchema.asSource())
    val transformer = exec.load()

    for ((name, value) <- params) {
      transformer.setParameter(name, new XdmAtomicValue(value.toString))
    }

    transformer.setInitialContextNode(sourceXML)
    result = new XdmDestination()
    transformer.setDestination(result)
    transformer.transform()

    val report = result.getXdmNode
    checkFailedAssertions(report)
  }

  private def getSchematronXSLT(filename: String): SAXSource = {
    val instream = getClass.getResourceAsStream("/etc/schematron/" + filename)
    if (instream == null) {
      throw new TestException("Failed to load " + filename + " from resources.")
    }
    new SAXSource(new InputSource(instream))
  }

  private def transform(source: XdmNode, stylesheet: SAXSource, schemaAware: Boolean): XdmNode = {
    val compiler = runtimeConfig.processor.newXsltCompiler()
    compiler.setSchemaAware(schemaAware)
    compiler.setURIResolver(uResolver)
    val exec = compiler.compile(stylesheet)
    val schemaCompiler = exec.load()
    schemaCompiler.setInitialContextNode(source)
    val result = new XdmDestination()
    schemaCompiler.setDestination(result)
    schemaCompiler.transform()
    result.getXdmNode
  }

  private def checkFailedAssertions(node: XdmNode): List[XdmNode] = {
    val nsBindings = mutable.HashMap.empty[String,String]
    nsBindings.put("svrl", "http://purl.oclc.org/dsdl/svrl")

    val xpath = "//svrl:failed-assert"
    val results = ListBuffer.empty[XdmNode]
    val config = runtimeConfig.processor.getUnderlyingConfiguration

    val xcomp = runtimeConfig.processor.newXPathCompiler()
    xcomp.setBaseURI(URI.create("http://example.com/"))
    for ((prefix,value) <- nsBindings) {
      xcomp.declareNamespace(prefix, value)
    }

    val xexec = xcomp.compile(xpath)
    val selector = xexec.load()

    selector.setContextItem(node)

    val values = selector.iterator()
    while (values.hasNext) {
      results += values.next.asInstanceOf[XdmNode]
    }

    results.toList
  }

  class UResolver extends URIResolver {
    def resolve(href: String, base: String): Source = {
      if (href == "iso_schematron_skeleton_for_saxon.xsl") {
        val skeleton = getClass.getResourceAsStream("/etc/schematron/iso_schematron_skeleton_for_saxon.xsl")
        if (skeleton == null) {
          throw new TestException("Failed to load iso_schematron_skeleton_for_saxon.xsl from resources")
        }
        new SAXSource(new InputSource(skeleton))
      } else {
        throw new TestException("Failed to resolve " + href)
      }
    }
  }
}
