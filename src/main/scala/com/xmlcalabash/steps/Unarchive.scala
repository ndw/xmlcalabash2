package com.xmlcalabash.steps

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import java.net.{URI, URLConnection}
import java.util.regex.Pattern

import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmValue}
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.IOUtils

import scala.collection.mutable.ListBuffer

class Unarchive extends DefaultXmlStep {
  private val _zip = new QName("", "zip")

  private val _relativeTo = new QName("", "relative-to")

  private var source: Any = _
  private var smeta: XProcMetadata = _

  private var format = Option.empty[QName]
  private var parameters = Map.empty[QName, XdmValue]
  private var relativeTo: URI = _
  private var includeFilter = ListBuffer.empty[String]
  private var excludeFilter = ListBuffer.empty[String]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

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

    if (bindings.get(_relativeTo).isDefined) {
      relativeTo = uriBinding(_relativeTo).get
    } else {
      smeta.baseURI.get
    }

    if (bindings.contains(XProcConstants._include_filter)) {
      val value = bindings(XProcConstants._include_filter)
      val iter = value.iterator()
      while (iter.hasNext) {
        includeFilter += iter.next().getStringValue
      }
    }

    if (bindings.contains(XProcConstants._exclude_filter)) {
      val value = bindings(XProcConstants._exclude_filter)
      val iter = value.iterator()
      while (iter.hasNext) {
        excludeFilter += iter.next().getStringValue
      }
    }

    format.get match {
      case `_zip` => unzip(context)
      case _ => ()
    }
  }

  private def unzip(context: StaticContext): Unit = {
    // ZIP requires random access: https://commons.apache.org/proper/commons-compress/zip.html
    source match {
      case bn: BinaryNode =>
        unzipFile(context, bn.file)
      case _ =>
        throw XProcException.xcArchiveFormatError(format.get, location)
    }
  }

  private def unzipFile(context: StaticContext, zfile: File): Unit = {
    val zipIn = new ZipFile(zfile)
    val enum = zipIn.getEntries
    while (enum.hasMoreElements) {
      val entry = enum.nextElement()

      if (!entry.isDirectory) {
        var matches = includeFilter.isEmpty
        for (regex <- includeFilter) {
          val patn = Pattern.compile(regex)
          matches = matches || patn.matcher(entry.getName).matches()
        }
        for (regex <- excludeFilter) {
          val patn = Pattern.compile(regex)
          matches = matches && !patn.matcher(entry.getName).matches()
        }

        if (matches) {
          if (zipIn.canReadEntryData(entry)) {
            val baos = new ByteArrayOutputStream()
            IOUtils.copy(zipIn.getInputStream(entry), baos)
            val bais = new ByteArrayInputStream(baos.toByteArray)

            val href = relativeTo.resolve(entry.getName)

            // Using the filename sort of sucks, but it's what the OSes do at this point so...sigh
            // You can extend the set of known extensions by pointing the system property
            // `content.types.user.table` at your own mime types file. See
            // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8039362
            val contentTypeString = Option(URLConnection.guessContentTypeFromName(href.toASCIIString)).getOrElse("application/octet-stream")

            val request = new DocumentRequest(href, MediaType.parse(contentTypeString))
            val response = config.documentManager.parse(request, bais)
            consumer.get.receive("result", response.value, new XProcMetadata(response.contentType))
          } else {
            logger.info(s"Cannot read {$entry.getName} from ZIP")
          }
        }

      }
    }
    zipIn.close()
  }

  private def inferredFormat(): Option[QName] = {
    val ctype = smeta.contentType.mediaType + "/" + smeta.contentType.mediaSubtype
    ctype match {
      case "application/zip" => Some(_zip)
      case _ => None
    }
  }
}