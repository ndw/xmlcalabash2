package com.xmlcalabash.steps.text

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import com.xmlcalabash.util.xc.XsltStylesheet
import net.sf.saxon.s9api.{QName, SaxonApiException, XdmDestination}

class Sort() extends TextLines {
  private val _sort_key = new QName("", "sort-key")
  private val _order = new QName("", "order")
  private val _case_order = new QName("", "case-order")
  private val _lang = new QName("", "lang")
  private val _collation = new QName("", "collation")
  private val _stable = new QName("", "stable")

  private val _sort = new QName("", "sort")
  private val _line = new QName("", "line")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  override def run(context: StaticContext): Unit = {
    val xslbuilder = new XsltStylesheet(config, context.nsBindings, List(), "2.0")

    xslbuilder.startVariable("lines", "element()*")
    for (line <- lines) {
      xslbuilder.literal(_line, line)
    }
    xslbuilder.endVariable()

    xslbuilder.startNamedTemplate("sort")
    xslbuilder.startForEach("$lines")

    val sort_key = optionalStringBinding(_sort_key)
    val order = optionalStringBinding(_order)
    val case_order = optionalStringBinding(_case_order)
    val lang = optionalStringBinding(_lang)
    val collation = optionalStringBinding(_collation)
    val stable = optionalStringBinding(_stable)

    if (order.isDefined && (order.get != "ascending" && order.get != "descending")) {
      throw XProcException.xdBadValue(order.get, location)
    }

    if (case_order.isDefined && (case_order.get != "upper-first" && case_order.get != "lower-first")) {
      throw XProcException.xdBadValue(case_order.get, location)
    }

    xslbuilder.startSort(sort_key.getOrElse("."), lang, order, collation, stable, case_order)

    xslbuilder.endSort()
    xslbuilder.valueOf(".")
    xslbuilder.text("\n")
    xslbuilder.endForEach()
    xslbuilder.endTemplate()

    val stylesheet = xslbuilder.endStylesheet()

    val processor = config.processor
    val compiler = processor.newXsltCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)

    val exec = try {
      compiler.compile(stylesheet.asSource())
    } catch {
      case ex: SaxonApiException =>
        throw XProcException.xcSortKeyError(location)
    }

    val transformer = exec.load()
    transformer.setInitialTemplate(_sort)

    val result = new XdmDestination()
    transformer.setDestination(result)
    try {
      transformer.transform()
    } catch {
      case _: SaxonApiException =>
        throw XProcException.xcSortError(location)
    }

    val xformed = result.getXdmNode

    consumer.get.receive("result", xformed, new XProcMetadata(MediaType.TEXT))
  }
}
