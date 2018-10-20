package com.xmlcalabash.messages

import java.io.{ByteArrayInputStream, File, FileInputStream, FileOutputStream, InputStream}

import com.xmlcalabash.runtime.{ExpressionContext, XProcMetadata}
import net.sf.saxon.s9api.XdmNode
import org.apache.http.util.ByteArrayBuffer
import org.slf4j.{Logger, LoggerFactory}

class AnyItemMessage(override val item: XdmNode,
                     private val shadowValue: Any,
                     override val metadata: XProcMetadata,
                     override val context: ExpressionContext) extends XProcItemMessage(item, metadata, context) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var stream: Option[InputStream] = None
  private var cache: Option[File] = None

  shadowValue match {
    case bytes: Array[Byte] =>
      if (bytes.length > 1024000) {
        makeFile(new ByteArrayInputStream(bytes))
      } else {
        logger.trace(s"Making binary stream (${bytes.length} bytes)")
        stream = Some(new ByteArrayInputStream(bytes))
      }
    case stream: InputStream =>
      // Make a copy of it, we don't know if this one's reusable or not
      makeFile(stream)
    case _ =>
      logger.trace(s"Caching binary (${shadow.getClass.getName}")
  }

  def shadow: Any = {
    if (cache.isDefined) {
      new FileInputStream(cache.get)
    } else if (stream.isDefined) {
      stream.get
    } else {
      shadowValue
    }
  }
  // FIXME: what about environments where writing to disk is not allowed?

  def this(item: XdmNode, shadow: Any, metadata: XProcMetadata) = {
    this(item, shadow, metadata, ExpressionContext.NONE)
  }

  private def makeFile(stream: InputStream): Unit = {
    val tempFile = File.createTempFile("xmlcalabash-", ".bin")
    tempFile.deleteOnExit()
    val fos = new FileOutputStream(tempFile)
    var totBytes = 0L
    val pagesize = 4096
    val buffer = new ByteArrayBuffer(pagesize)
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
    logger.trace(s"Making binary stream on disk ($totBytes bytes)")
    cache = Some(tempFile)
  }
}
