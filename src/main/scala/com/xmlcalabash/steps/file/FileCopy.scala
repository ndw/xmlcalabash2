package com.xmlcalabash.steps.file

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.stores.{DataReader, DataWriter}
import com.xmlcalabash.util.{MediaType, URIUtils}
import net.sf.saxon.s9api.QName

import java.io.{InputStream, OutputStream}
import java.net.URI
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, LinkOption, Path, Paths, StandardCopyOption}

class FileCopy() extends DefaultXmlStep {
  private val _target = new QName("", "target")
  private val _overwrite = new QName("", "overwrite")
  private val _fail_on_error = new QName("", "fail-on-error")
  private val cx_copyLinks = new QName("cx", XProcConstants.ns_cx, "copy-links")
  private val cx_copyAttributes = new QName("cx", XProcConstants.ns_cx,"copy-attributes")
  private val _bufsize = 8192

  private var overwrite = true
  private var failOnError = true
  private var copyLinks = false
  private var copyAttributes = false

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    val href = UriBinding(XProcConstants._href).get
    val target = UriBinding(_target).get

    overwrite = booleanBinding(_overwrite).getOrElse(overwrite);
    failOnError = booleanBinding(_fail_on_error).getOrElse(failOnError)
    copyLinks = booleanBinding(cx_copyLinks).getOrElse(copyLinks)
    copyAttributes = booleanBinding(cx_copyAttributes).getOrElse(copyAttributes)

    if (href.getScheme == "file") {
      val srcpath = Paths.get(href.getPath)
      if (Files.isDirectory(srcpath)) {
        if (target.getScheme == "file") {
          copyDirectory(srcpath, Paths.get(target.getPath).resolve(srcpath.getFileName))
          return
        } else {
          throw new RuntimeException("can't copy dir to non-file URI")
        }
      }
    }

    config.datastore.readEntry(href.toString, href, "*/*", None, new CopyReader(target))

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(URIUtils.cwdAsURI)
    builder.addStartElement(XProcConstants.c_result)
    builder.addText(href.toString)
    builder.addText("\n")
    builder.addText(target.toString)
    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }

  def copyDirectory(source: Path, target: Path): Unit = {
    if (!Files.exists(target)) {
      val permissions = Files.getPosixFilePermissions(source)
      val fileattr = PosixFilePermissions.asFileAttribute(permissions)
      Files.createDirectories(target, fileattr)
    }

    if (!Files.exists(target)) {
      throw new RuntimeException("failed to create " + target);
    }

    if (!Files.isDirectory(target)) {
      throw XProcException.xcCopyDirToFile(source.toString, target.toString, location);
    }

    Files.walk(source).forEach(copyItem(source, target, _))
  }

  private def copyItem(source: Path, target: Path, item: Path): Unit = {
    if (source == item) {
      // .walk passes the source as the first item, ignore this
      return
    }

    val relsrc = source.relativize(item)
    val output = target.resolve(relsrc)
    if (copyLinks && Files.isSymbolicLink(item)) {
      try {
        if (Files.exists(output, LinkOption.NOFOLLOW_LINKS) && overwrite) {
          Files.delete(output);
        }
        Files.createSymbolicLink(output, Files.readSymbolicLink(item))
      } catch {
        case ex: Exception =>
          if (failOnError) {
            throw ex;
          } else {
            logger.info("Failed to create symlink: " + ex.getMessage)
          }
      }
    } else if (Files.isDirectory(item)) {
      if (!Files.exists(output)) {
        val permissions = Files.getPosixFilePermissions(item)
        val fileattr = PosixFilePermissions.asFileAttribute(permissions)
        Files.createDirectories(output, fileattr)
      }
    } else if (Files.isRegularFile(item) || (Files.isSymbolicLink(item) && !copyLinks)) {
      try {
        if (overwrite) {
          if (copyAttributes) {
            Files.copy(item, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
          } else {
            Files.copy(item, output, StandardCopyOption.REPLACE_EXISTING)
          }
        } else {
          if (copyAttributes) {
            Files.copy(item, output, StandardCopyOption.COPY_ATTRIBUTES)
          } else {
            Files.copy(item, output)
          }
        }
      } catch {
        case ex: Exception =>
          if (failOnError) {
            throw ex;
          } else {
            logger.info("Failed to copy file: " + ex.getMessage)
          }
      }
    } else {
      logger.info("Ignoring special file: " + item);
    }
  }

  private class CopyReader(target: URI) extends DataReader {
    override def load(id: URI, media: String, content: InputStream, len: Option[Long]): Unit = {
      config.datastore.writeEntry(target.toString, target, "application/octet-stream", new CopyWriter(content))
    }
  }

  private class CopyWriter(source: InputStream) extends DataWriter {
    override def store(content: OutputStream): Unit = {
      var buf = source.readNBytes(_bufsize)
      while (buf.nonEmpty) {
        content.write(buf);
        buf = source.readNBytes(_bufsize)
      }
    }
  }
}
