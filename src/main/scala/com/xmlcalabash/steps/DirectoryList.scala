package com.xmlcalabash.steps

import java.net.URI

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.stores.{DataInfo, FallbackDataStore, FileDataStore}
import com.xmlcalabash.util.{MediaType, URIUtils}
import net.sf.saxon.s9api.{QName, XdmAtomicValue}

class DirectoryList() extends DefaultXmlStep {
  private val _path = new QName("", "path")
  private val FILE = new XdmAtomicValue("file")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(URIUtils.cwdAsURI)
    builder.addStartElement(XProcConstants.c_directory)
    builder.startContent()

    val path = bindings(_path).getStringValue
    val fileDS = new FileDataStore(new FallbackDataStore())

    // cwd() is wrong! should be base URI of the step
    fileDS.listEachEntry(path, URIUtils.cwdAsURI, "*/*", new DataInfo() {
      override def list(id: URI, props: Map[String, XdmAtomicValue]): Unit = {
        props.getOrElse("file-type", FILE).toString match {
          case "file" => builder.addStartElement(XProcConstants.c_file)
          case "directory" =>builder.addStartElement(XProcConstants.c_directory)
          case _ => builder.addStartElement(XProcConstants.c_other)
        }

        builder.addAttribute(XProcConstants._name, id.toASCIIString)
        for ((key, value) <- props) {
          if (key != "file-type") {
            builder.addAttribute(new QName("", key), value.toString)
          }
        }
        builder.startContent()
        builder.addEndElement()
      }
    })

    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }
}
