package com.xmlcalabash.model.xml

import com.xmlcalabash.util.{PipelineErrorListener, SourceLocation}
import net.sf.saxon.s9api.{QName, XdmNode}
import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/16/16.
  */
class XMLErrorListener(val listener: Option[PipelineErrorListener]) extends PipelineErrorListener {
  val cwd = System.getProperty("user.dir") + "/"

  def this() {
    this(None)
  }
  def this(parentListener: PipelineErrorListener) {
    this(Some(parentListener))
  }

  val logger = LoggerFactory.getLogger(this.getClass)

  override def error(code: QName, loc: SourceLocation, msg: String): Unit = {
    if (listener.isDefined) {
      listener.get.error(code, loc, msg)
    } else {
      error(Some(code), Some(loc), msg)
    }
  }

  def error(code: QName, node: Option[XdmNode], msg: String): Unit = {
    if (node.isEmpty) {
      error(code, msg)
    } else {
      if (listener.isDefined) {
        listener.get.error(code, new SourceLocation(node.get), msg)
      } else {
        error(code, new SourceLocation(node.get), msg)
      }
    }
  }

  override def error(code: QName, msg: String): Unit = {
    if (listener.isDefined) {
      listener.get.error(code, msg)
    } else {
      error(Some(code), None, msg)
    }
  }

  override def error(loc: SourceLocation, msg: String): Unit = {
    if (listener.isDefined) {
      listener.get.error(loc, msg)
    } else {
      error(None, Some(loc), msg)
    }
  }

  def error(node: Option[XdmNode], msg: String): Unit = {
    if (node.isEmpty) {
      error(msg)
    } else {
      if (listener.isDefined) {
        listener.get.error(new SourceLocation(node.get), msg)
      } else {
        error(new SourceLocation(node.get), msg)
      }
    }
  }

  override def error(msg: String): Unit = {
    if (listener.isDefined) {
      listener.get.error(msg)
    } else {
      error(None, None, msg)
    }
  }

  override def warn(code: QName, loc: SourceLocation, msg: String): Unit = {
    if (listener.isDefined) {
      listener.get.warn(code, loc, msg)
    } else {
      warn(Some(code), Some(loc), msg)
    }
  }

  def warn(code: QName, node: Option[XdmNode], msg: String): Unit = {
    if (node.isEmpty) {
      warn(code, msg)
    } else {
      if (listener.isDefined) {
        listener.get.warn(code, new SourceLocation(node.get), msg)
      } else {
        warn(code, new SourceLocation(node.get), msg)
      }
    }
  }

  override def warn(code: QName, msg: String): Unit = {
    if (listener.isDefined) {
      listener.get.warn(code, msg)
    } else {
      warn(Some(code), None, msg)
    }
  }

  override def warn(loc: SourceLocation, msg: String): Unit = {
    if (listener.isDefined) {
      listener.get.warn(loc, msg)
    } else {
      warn(None, Some(loc), msg)
    }
  }

  def warn(node: Option[XdmNode], msg: String): Unit = {
    if (node.isEmpty) {
      warn(msg)
    } else {
      if (listener.isDefined) {
        listener.get.warn(new SourceLocation(node.get), msg)
      } else {
        warn(new SourceLocation(node.get), msg)
      }
    }
  }

  override def warn(msg: String): Unit = {
    if (listener.isDefined) {
      listener.get.warn(msg)
    } else {
      warn(None, None, msg)
    }
  }

  private def format(code: Option[QName], loc: Option[SourceLocation], msg: String): String = {
    var formatted = ""
    if (loc.isDefined) {
      var path = loc.get.baseURI.toASCIIString
      if (path.startsWith("file:" + cwd) || path.startsWith("file://" + cwd)) {
        val pos = path.indexOf(cwd)
        path = path.substring(pos + cwd.length)
      }
      formatted += path + ":"
      if (loc.get.lineNumber > 0) {
        formatted += loc.get.lineNumber + ":" + loc.get.columnNumber + ":"
      }
    }
    if (code.isDefined) {
      formatted += code.toString + ":"
    }
    formatted + msg
  }


  def error(code: Option[QName], loc: Option[SourceLocation], msg: String): Unit = {
    logger.error(format(code, loc, msg))
  }

  def warn(code: Option[QName], loc: Option[SourceLocation], msg: String): Unit = {
    logger.warn(format(code, loc, msg))
  }
}
