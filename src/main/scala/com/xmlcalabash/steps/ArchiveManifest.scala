package com.xmlcalabash.steps

import java.io.InputStream
import java.net.URI
import java.util.zip.{ZipEntry, ZipInputStream}

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmValue}

class ArchiveManifest extends DefaultXmlStep {
  private val _zip = new QName("", "zip")
  private val _jar = new QName("", "jar")

  private val _relativeTo = new QName("", "relative-to")

  private var source: Any = _
  private var smeta: XProcMetadata = _

  private var format = Option.empty[QName]
  private var parameters = Map.empty[QName, XdmValue]
  private var relativeTo = Option.empty[URI]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYXML

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item
    smeta = metadata
  }

  override def receiveBinding(variable: QName, value: XdmValue, context: StaticContext): Unit = {
    if (variable == XProcConstants._parameters) {
      if (value.size() > 0) {
        parameters = ValueParser.parseParameters(value, context)
      }
    } else {
      super.receiveBinding(variable, value, context)
    }
  }

  override def run(context: StaticContext): Unit = {
    format = if (qnameBinding(XProcConstants._format).isDefined) {
      qnameBinding(XProcConstants._format)
    } else {
      inferredFormat()
    }

    if (format.isEmpty) {
      throw XProcException.xcUnrecognizedArchiveFormat(location)
    }

    if (format.get != _zip) {
      throw XProcException.xcUnknownArchiveFormat(format.get, location)
    }

    relativeTo = optionalURIBinding(_relativeTo)

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(smeta.baseURI)
    builder.startContent()
    builder.addStartElement(XProcConstants.c_archive)
    builder.startContent()

    format.get match {
      case _zip => zipArchive(context, builder)
      case _ => Unit
    }

    builder.addEndElement()
    builder.endDocument()
    val result = builder.result

    consumer.get.receive("result", result, new XProcMetadata(MediaType.XML))
  }

  private def zipArchive(context: StaticContext, builder: SaxonTreeBuilder): Unit = {
    source match {
      case bn: BinaryNode =>
        zipArchiveStream(context, builder, bn.stream)
      case is: InputStream =>
        zipArchiveStream(context, builder, is)
      case _ =>
        throw XProcException.xcBadArchiveFormat(format.get, location)
    }
  }

  private def zipArchiveStream(context: StaticContext, builder: SaxonTreeBuilder, is: InputStream): Unit = {
    val zipIn = new ZipInputStream(is)
    var entry = Option(zipIn.getNextEntry)
    while (entry.isDefined) {
      if (!entry.get.isDirectory) {
        builder.addStartElement(XProcConstants.c_entry)
        builder.addAttribute(XProcConstants._name, entry.get.getName)
        if (relativeTo.isDefined) {
          builder.addAttribute(XProcConstants._href, relativeTo.get.resolve(entry.get.getName).toASCIIString)
        } else {
          if (context.baseURI.isDefined) {
            builder.addAttribute(XProcConstants._href, context.baseURI.get.resolve(entry.get.getName).toASCIIString)
          } else {
            // I wouldn't expect this to succeed, as the name is unlikely to be an absolute
            // URI, but I've run out of options.
            builder.addAttribute(XProcConstants._href, new URI(entry.get.getName).toASCIIString)
          }
        }

        archiveAttribute(builder, XProcConstants._comment, Option(entry.get.getComment))
        entry.get.getMethod match {
          case ZipEntry.STORED => archiveAttribute(builder, XProcConstants._method, Some("none"))
          case ZipEntry.DEFLATED => archiveAttribute(builder, XProcConstants._method, Some("deflated"))
          case _ => archiveAttribute(builder, XProcConstants._method, Some("unknown"))
        }
        archiveAttribute(builder, XProcConstants._compressed_size, Some(entry.get.getCompressedSize.toString))
        archiveAttribute(builder, XProcConstants._size, Some(entry.get.getSize.toString))
        builder.startContent()
        builder.addEndElement()
      }
      entry = Option(zipIn.getNextEntry)
    }
    zipIn.close()
  }

  private def archiveAttribute(builder: SaxonTreeBuilder, name: QName, value: Option[String]): Unit = {
    if (value.isDefined) {
      builder.addAttribute(name, value.get)
    }
  }

  private def inferredFormat(): Option[QName] = {
    val ctype = smeta.contentType.mediaType + "/" + smeta.contentType.mediaSubtype
    ctype match {
      case "application/zip" => Some(_zip)
      case "application/jar+archive" => Some(_jar)
      case _ => None
    }
  }
}