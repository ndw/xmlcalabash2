package com.xmlcalabash.steps.text

import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import com.xmlcalabash.util.xc.XsltStylesheet
import net.sf.saxon.s9api.{QName, XdmDestination}

class Sort() extends TextLines {
  private val _order = new QName("", "order")
  private val _case_order = new QName("", "case-order")
  private val _lang = new QName("", "lang")
  private val _data_type = new QName("", "data-type")
  private val _collation = new QName("", "collation")
  private val _stable = new QName("", "stable")
  private val _sort = new QName("", "sort")
  private val _line = new QName("", "line")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  private def svalue(key: QName): Option[String] = {
    if (bindings.contains(key)) {
      Some(bindings(key).getStringValue)
    } else {
      None
    }
  }

  override def run(context: StaticContext): Unit = {
    val xslbuilder = new XsltStylesheet(config)

    xslbuilder.startVariable("lines", "element()*")
    for (line <- lines) {
      xslbuilder.literal(_line, line)
    }
    xslbuilder.endVariable()

    xslbuilder.startNamedTemplate("sort")
    xslbuilder.startForEach("$lines")
    xslbuilder.startSort(".", svalue(_lang), svalue(_order), svalue(_collation),
      svalue(_stable), svalue(_case_order), svalue(_data_type))
    xslbuilder.endSort()
    xslbuilder.valueOf(".")
    xslbuilder.text("\n")
    xslbuilder.endForEach()
    xslbuilder.endTemplate()

    val stylesheet = xslbuilder.endStylesheet()

    val processor = config.processor
    val compiler = processor.newXsltCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)
    val exec = compiler.compile(stylesheet.asSource())
    val transformer = exec.load()
    transformer.setInitialTemplate(_sort)

    val result = new XdmDestination()
    transformer.setDestination(result)
    transformer.transform()

    val xformed = result.getXdmNode

    consumer.get.receive("result", xformed, new XProcMetadata(MediaType.TEXT))
  }
}
