package com.xmlcalabash.steps

import java.io.ByteArrayInputStream

import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

class Markdown() extends DefaultXmlStep {
  private var parameters = Map.empty[QName, XdmValue]
  var markdown: Option[XdmNode] = None

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.HTMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (port == "source") {
      item match {
        case node: XdmNode => markdown = Some(node)
      }
    }
  }

  override def receiveBinding(variable: QName, value: XdmValue, context: ExpressionContext): Unit = {
    if (variable == XProcConstants._parameters) {
      parameters = ValueParser.parseParameters(value, context.nsBindings, context.location)
    }
  }

  override def run(context: StaticContext): Unit = {
    val parser = Parser.builder.build
    val document = parser.parse(markdown.get.getStringValue)
    val renderer = HtmlRenderer.builder.build

    // We rely on the fact that the CommonMark parser returns well-formed markup consisting
    // of the paragraphs and other bits that would occur inside a <body> element and
    // that it returns them with no namespace declarations.
    val markup = "<body xmlns='http://www.w3.org/1999/xhtml'>" + renderer.render(document) + "</body>"

    val request = new DocumentRequest(markdown.get.getBaseURI, Some(MediaType.HTML), location, false)
    request.params = parameters.toMap

    val stream = new ByteArrayInputStream(markup.getBytes)

    val result = config.documentManager.parse(request, stream)

    consumer.get.receive("result", result.value, new XProcMetadata(MediaType.HTML))
  }
}
