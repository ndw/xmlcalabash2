package com.xmlcalabash.steps

import java.net.URI

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.stores.{DataInfo, FallbackDataStore, FileDataStore}
import com.xmlcalabash.util.{MediaType, URIUtils}
import net.sf.saxon.s9api.{QName, XdmAtomicValue}

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

class DirectoryList() extends DefaultXmlStep {
  private val _path = new QName("", "path")
  private val _detailed = new QName("", "detailed")
  private val _include_filter = new QName("", "include-filter")
  private val _exclude_filter = new QName("", "exclude-filter")
  private val FILE = new XdmAtomicValue("file")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(URIUtils.cwdAsURI)
    builder.addStartElement(XProcConstants.c_directory)
    builder.startContent()

    val path = bindings(_path).getStringValue
    val detailed = bindings(_detailed).getStringValue == "true"
    val fileDS = new FileDataStore(new FallbackDataStore())
    val include = ListBuffer.empty[Regex]
    val exclude = ListBuffer.empty[Regex]

    if (bindings.contains(_include_filter)) {
      val filter = bindings(_include_filter)
      val iter = filter.value.iterator()
      while (iter.hasNext) {
        val item = iter.next()
        include += new Regex(item.getStringValue)
      }
    }

    if (bindings.contains(_exclude_filter)) {
      val filter = bindings(_exclude_filter)
      val iter = filter.value.iterator()
      while (iter.hasNext) {
        val item = iter.next()
        exclude += new Regex(item.getStringValue)
      }
    }

    fileDS.listEachEntry(path, context.baseURI.getOrElse(URIUtils.cwdAsURI), "*/*", new DataInfo() {
      override def list(id: URI, props: Map[String, XdmAtomicValue]): Unit = {
        var filename = id.getPath
        if (filename.endsWith("/")) {
          filename = filename.substring(0, filename.length - 1)
        }
        val pos = filename.lastIndexOf("/")
        if (pos >= 0) {
          filename = filename.substring(pos+1)
        }

        var rel = Option.empty[String]
        var rematch = true
        if (include.nonEmpty) {
          rematch = false
          for (patn <- include) {
            filename match {
              case patn(_*) =>
                if (!rematch) {
                  rel = Some(patn.toString())
                }
                rematch = true
              case _ => Unit
            }
          }
        }

        for (patn <- exclude) {
          filename match {
            case patn(_*) =>
              if (rematch) {
                rel = Some(patn.toString())
              }
              rematch = false
            case _ => Unit
          }
        }

        if (rel.isDefined) {
          if (rematch) {
            logger.trace(s"Include $filename (matches ${rel.get})")
          } else {
            logger.trace(s"Exclude $filename (matches ${rel.get})")
          }
        }

        if (rematch) {
          props.getOrElse("file-type", FILE).toString match {
            case "file" => builder.addStartElement(XProcConstants.c_file)
            case "directory" =>builder.addStartElement(XProcConstants.c_directory)
            case _ => builder.addStartElement(XProcConstants.c_other)
          }

          builder.addAttribute(XProcConstants._name, id.toASCIIString)
          if (detailed) {
            for ((key, value) <- props) {
              if (key != "file-type") {
                builder.addAttribute(new QName("", key), value.toString)
              }
            }
          }
          builder.startContent()
          builder.addEndElement()
        }
      }
    })

    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }
}
