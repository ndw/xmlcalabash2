package com.xmlcalabash.steps

import java.io.{File, InputStream}
import java.net.URI
import java.util.zip.ZipEntry
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.{QName, XdmValue}
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipFile}

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
    builder.addStartElement(XProcConstants.c_archive)

    format.get match {
      case `_zip` => zipArchive(context, builder)
      case _ => ()
    }

    builder.addEndElement()
    builder.endDocument()
    val result = builder.result

    consumer.get.receive("result", result, new XProcMetadata(MediaType.XML))
  }

  private def zipArchive(context: StaticContext, builder: SaxonTreeBuilder): Unit = {
    // ZIP requires random access: https://commons.apache.org/proper/commons-compress/zip.html
    source match {
      case bn: BinaryNode =>
        zipArchiveFile(context, builder, bn.file)
      case _ =>
        throw XProcException.xcArchiveFormatError(format.get, location)
    }
  }

  private def zipArchiveFile(context: StaticContext, builder: SaxonTreeBuilder, zfile: File): Unit = {
    val zipIn = new ZipFile(zfile)
    val enum = zipIn.getEntries
    while (enum.hasMoreElements) {
      val entry = enum.nextElement()

      if (!entry.isDirectory) {
        var amap: AttributeMap = EmptyAttributeMap.getInstance()
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, entry.getName))
        if (relativeTo.isDefined) {
          amap = amap.put(TypeUtils.attributeInfo(XProcConstants._href, relativeTo.get.resolve(entry.getName).toASCIIString))
        } else {
          if (context.baseURI.isDefined) {
            amap = amap.put(TypeUtils.attributeInfo(XProcConstants._href, context.baseURI.get.resolve(entry.getName).toASCIIString))
          } else {
            // I wouldn't expect this to succeed, as the name is unlikely to be an absolute
            // URI, but I've run out of options.
            amap = amap.put(TypeUtils.attributeInfo(XProcConstants._href, new URI(entry.getName).toASCIIString))
          }
        }


        amap = archiveAttribute(amap, XProcConstants._comment, Option(entry.getComment))
        amap = entry.getMethod match {
          // ZipArchiveEntry inherites from ZipEntry, but ZipArchiveEntry.STORED doesn't resolve???
          case ZipEntry.STORED => archiveAttribute(amap, XProcConstants._method, Some("none"))
          case ZipEntry.DEFLATED => archiveAttribute(amap, XProcConstants._method, Some("deflated"))
          case _ => archiveAttribute(amap, XProcConstants._method, Some("unknown"))
        }
        if (Option(entry.getComment).isDefined) {
          amap = archiveAttribute(amap, XProcConstants._comment, Some(entry.getComment))
        }
        if (entry.getCompressedSize >= 0) {
          amap = archiveAttribute(amap, XProcConstants._compressed_size, Some(entry.getCompressedSize.toString))
        }
        if (entry.getSize >= 0) {
          amap = archiveAttribute(amap, XProcConstants._size, Some(entry.getSize.toString))
        }

        builder.addStartElement(XProcConstants.c_entry, amap)
        builder.addEndElement()
      }
    }
    zipIn.close()
  }

  private def archiveAttribute(amap: AttributeMap, name: QName, value: Option[String]): AttributeMap = {
    if (value.isDefined) {
      amap.put(TypeUtils.attributeInfo(name, value.get))
    } else {
      amap
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