package com.xmlcalabash.steps

import java.io.{ByteArrayInputStream, StringBufferInputStream, StringReader}

import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api}
import javax.xml.transform.sax.SAXSource
import net.sf.saxon.s9api._
import org.xml.sax.InputSource

class UnescapeMarkup() extends DefaultXmlStep {
  private val _content_type = new QName("content-type")
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    metadata = meta
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val text = "<wrapper>" + source.getStringValue + "</wrapper>"
    val stream = new ByteArrayInputStream(text.getBytes("utf-8"))

    val contentType = MediaType.parse(bindings(_content_type).getStringValue)
    val request = new DocumentRequest(source.getBaseURI, contentType, context.location)
    val resp = config.documentManager.parse(request, stream)

    val root = S9Api.documentElement(source)
    if (root.isEmpty) {
      throw new RuntimeException("Input is not an XML document")
    }

    val tree = new SaxonTreeBuilder(config)
    tree.startDocument(source.getBaseURI)
    tree.addStartElement(root.get)
    tree.addAttributes(root.get)
    tree.startContent()
    resp.value match {
      case node: XdmNode =>
        tree.addSubtree(node)
      case _ =>
        tree.addText(resp.value.getUnderlyingValue.getStringValue)
    }
    tree.endDocument()

    consumer.get.receive("result", tree.result, metadata)
  }
}
