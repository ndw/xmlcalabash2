package com.xmlcalabash.steps.file

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.stores.{DataInfo, DataReader, DataWriter}
import com.xmlcalabash.util.{InternetProtocolRequest, MediaType, URIUtils}
import net.sf.saxon.s9api.{QName, XdmAtomicValue}

import java.io.{IOException, InputStream, OutputStream}
import java.net.URI
import java.nio.file.attribute.{BasicFileAttributes, PosixFilePermissions}
import java.nio.file.{FileVisitResult, Files, LinkOption, Path, Paths, SimpleFileVisitor, StandardCopyOption}

class FileDelete() extends DefaultXmlStep {
  private var href: URI = _
  private var recursive = false
  private var failOnError = true

  private var staticContext: StaticContext = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    staticContext = context
    href = uriBinding(XProcConstants._href).get
    recursive = booleanBinding(XProcConstants._recursive).getOrElse(recursive)
    failOnError = booleanBinding(XProcConstants._fail_on_error).getOrElse(failOnError)

    try {
      if (href.getScheme == "file") {
        deleteFile(href)
      } else if (href.getScheme == "http" || href.getScheme == "https") {
        deleteHttp(href)
      } else {
        throw XProcException.xcFileDeleteBadScheme(href, location);
      }
    } catch {
      case ex: Exception =>
        if (failOnError) {
          throw ex
        }
        logger.info("Failed to delete " + href);
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(URIUtils.cwdAsURI)
    builder.addStartElement(XProcConstants.c_result)
    builder.addText(href.toString)
    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }

  private def deleteFile(href: URI): Unit = {
    val path = Paths.get(href.getPath)
    if (!Files.exists(path)) {
      return
    }

    if (Files.isDirectory(path)) {
      deleteDirectory(path)
    } else {
      Files.delete(path)
    }
  }

  private def deleteDirectory(path: Path): Unit = {
    if (Files.list(path).findAny.isPresent) {
      if (!recursive) {
        throw XProcException.xcFileDeleteNotRecursive(href, location)
      }
      Files.walkFileTree(path, new DeleteVisitor())
    } else {
      Files.delete(path)
    }
  }

  private def deleteHttp(href: URI): Unit = {
    val request = new InternetProtocolRequest(config, staticContext, href)
    val response = request.execute("DELETE")
    if (response.statusCode.getOrElse(204) >= 400) {
      throw new RuntimeException("Failed to delete " + href)
    }
  }

  private class DeleteVisitor extends SimpleFileVisitor[Path] {
    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      try {
        Files.delete(file)
      } catch {
        case ex: Exception =>
          if (failOnError) {
            throw ex
          }
          logger.info("Failed to delete " + file)
      }
      FileVisitResult.CONTINUE
    }
    override def postVisitDirectory(dir: Path, ex: IOException): FileVisitResult = {
      try {
        Files.delete(dir)
      } catch {
        case ex: Exception =>
          if (failOnError) {
            throw ex
          }
          logger.info("Failed to delete " + dir)
      }
      FileVisitResult.CONTINUE
    }
  }
}