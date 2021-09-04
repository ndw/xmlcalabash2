package com.xmlcalabash.steps.file

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.QName

import java.net.URI
import java.nio.file.{Files, Paths, StandardCopyOption}

class FileMove() extends FileStep {
  private var failOnError = true
  private var overwrite = true

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    val href = uriBinding(XProcConstants._href).get
    var target = uriBinding(XProcConstants._target).get
    failOnError = booleanBinding(XProcConstants._fail_on_error).getOrElse(failOnError)
    overwrite = booleanBinding(XProcConstants._overwrite).getOrElse(overwrite)
    var exception = Option.empty[Exception]

    try {
      if (href.getScheme != "file") {
        throw XProcException.xcFileMoveBadScheme(href, location);
      }
      if (target.getScheme != "file") {
        throw XProcException.xcFileMoveBadScheme(target, location);
      }

      val source = Paths.get(href.getPath)
      var dest = Paths.get(target.getPath)

      if (!Files.exists(source)) {
        throw XProcException.xdDoesNotExist(href.toString, location)
      }

      if (Files.exists(dest)) {
        if (Files.isDirectory(dest)) {
          dest = dest.resolve(source.getFileName)
          target = new URI("file://" + dest)
        } else {
          if (Files.isDirectory(source)) {
            throw XProcException.xcFileMoveDirToFile(href, target, location)
          }
        }
      }

      if (overwrite) {
        Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING)
      } else {
        Files.move(source, dest)
      }
    } catch {
      case ex: Exception =>
        if (failOnError) {
          throw ex
        }
        exception = Some(ex)
        logger.info("Failed to move " + href + " to " + target);
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)

    if (exception.isDefined) {
      errorFromException(builder, exception.get)
    } else {
      builder.addStartElement(XProcConstants.c_result)
      builder.addText(target.toString)
      builder.addEndElement()
    }

    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }
}
