package com.xmlcalabash.steps.os

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer

class OsExec extends DefaultXmlStep {
  private val _command = new QName("", "command")
  private val _args = new QName("", "args")
  private val _cwd = new QName("", "cwd")
  private val _result_content_type = new QName("", "result-content-type")
  private val _error_content_type = new QName("", "error-content-type")
  private val _path_separator = new QName("", "path-separator")
  private val _failure_threshold = new QName("", "failure-threshold")

  private var command = ""
  private var args = ListBuffer.empty[String]
  private var cwd = Option.empty[String]
  private var resultContentType = "text/plain"
  private var errorContentType = "text/plain"
  private var pathSeparator = Option.empty[String]
  private var failureThreshold = Option.empty[Int]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.ZERO_OR_MORE, "error" -> PortCardinality.EXACTLY_ONE, "exit-status" -> PortCardinality.EXACTLY_ONE),
    Map("result" -> List("*"), "error" -> List("*"), "exit-status" -> List("application/xml")))

  override def run(context: StaticContext): Unit = {
    command = stringBinding(_command)
    cwd = optionalStringBinding(_cwd)

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)
    builder.addStartElement(XProcConstants.c_result)

    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }
}
