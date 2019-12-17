package com.xmlcalabash.runtime

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileInputStream, FileOutputStream, InputStream}

import org.slf4j.{Logger, LoggerFactory}

class BinaryNode(config: XMLCalabashRuntime, private val rawValue: Any) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var cacheBytes = Option.empty[Array[Byte]]
  private var cacheFile = Option.empty[File]

  rawValue match {
    case stream: ByteArrayOutputStream =>
      val bytes = stream.toByteArray
      if (bytes.length > 1024000) {
        makeFile(new ByteArrayInputStream(bytes))
      } else {
        logger.trace(s"Storing ${bytes.length} bytes)")
        cacheBytes = Some(bytes)
      }
    case bytes: Array[Byte] =>
      if (bytes.length > 1024000) {
        makeFile(new ByteArrayInputStream(bytes))
      } else {
        logger.trace(s"Storing ${bytes.length} bytes)")
        cacheBytes = Some(bytes)
      }
    case stream: InputStream =>
      // Make a copy of it, we don't know if this one's reusable or not
      makeFile(stream)
    case _ =>
      logger.trace(s"Caching binary (${rawValue.getClass.getName}")
  }

  def value: Any = rawValue

  def stream: InputStream = {
    if (cacheBytes.isDefined) {
      new ByteArrayInputStream(cacheBytes.get)
    } else if (cacheFile.isDefined) {
      new FileInputStream(cacheFile.get)
    } else {
      throw new RuntimeException(s"No InputStream support for BinaryNode raw value: ${rawValue.getClass.getName}")
    }
  }

  // If you attempt to access a file, we force the binary node into one
  def file: File = {
    if (cacheFile.isDefined) {
      cacheFile.get
    } else {
      makeFile(stream)
      cacheFile.get
    }
  }

  private def makeFile(stream: InputStream): Unit = {
    val tempFile = File.createTempFile("xmlcalabash-", ".bin")
    tempFile.deleteOnExit()
    val fos = new FileOutputStream(tempFile)
    var totBytes = 0L
    val pagesize = 4096
    val tmp = new Array[Byte](4096)
    var length = 0
    length = stream.read(tmp)
    while (length >= 0) {
      fos.write(tmp, 0, length)
      totBytes += length
      length = stream.read(tmp)
    }
    fos.close()
    stream.close()
    logger.trace(s"Storing $totBytes bytes bytes in $tempFile")
    cacheBytes = None
    cacheFile = Some(tempFile)
  }
}
