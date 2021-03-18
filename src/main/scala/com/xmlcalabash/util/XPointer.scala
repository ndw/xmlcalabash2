package com.xmlcalabash.util

import java.io.BufferedReader
import java.util.regex.{Matcher, Pattern}

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XPointer(runtime: XMLCalabashRuntime, xpointer: String, readLimit: Int) {
  private val _xmlns = new QName("xmlns")
  private val _element = new QName("element")
  private val _xpath = new QName("xpath")
  private val _text = new QName("text")
  private val _search = new QName("search")

  private val parts = ListBuffer.empty[XPointerScheme]
  private val pointer = parse(xpointer)

  def xpathNamespaces: Map[String,String] = {
    val bindings = mutable.HashMap.empty[String,String]
    for (scheme <- parts) {
      if (scheme.schemeName == _xmlns) {
        val xmlns = scheme.asInstanceOf[XPointerXmlnsScheme]
        bindings.put(xmlns.getPrefix, xmlns.getURI)
      }
    }
    bindings.toMap
  }

  def selectNodes(runtime: XMLCalabashRuntime, doc: XdmNode): List[XdmNode] = {
    val result = ListBuffer.empty[XdmNode]

    for (scheme <- parts) {
      val select = scheme.xpathEquivalent
      if (result.isEmpty && select.isDefined) {
        try {
          result ++= scheme.selectNodes(runtime, doc, xpathNamespaces)
        } catch {
          case _: XProcException => ()
        }
      }
    }

    result.toList
  }

  def selectText(stream: BufferedReader, contentLength: Int): Option[String] = {
    var result = Option.empty[String]
    var goesBang = Option.empty[Exception]

    for (scheme <- parts) {
      val select = scheme.textEquivalent
      if (result.isEmpty && select.isDefined) {
        try {
          if (select.get.startsWith("search=")) {
            result = Some(scheme.selectSearchText(stream, contentLength))
          } else {
            result = Some(scheme.selectText(stream, contentLength))
          }
        } catch {
          case iae: IllegalArgumentException =>
            goesBang = Some(iae)
            result = None
          case xe: XProcException =>
            goesBang = Some(xe)
            result = None
        }
      }
    }

    if (result.isEmpty && goesBang.isDefined) {
      throw goesBang.get
    }

    result
  }

  private def parse(pointer: String): Option[String] = {
    var xpointer = pointer

    // FIXME: Hack! Is this acceptable?
    if (xpointer.startsWith("/") && !xpointer.contains("(")) {
      xpointer = "element(" + xpointer + ")"
    } else {
      try {
        val tutils = new TypeUtils(runtime)
        tutils.checkType(xpointer, XProcConstants.xs_NCName)
        xpointer = "element(" + xpointer + ")"
      } catch {
        case _: Exception => ()
      }
    }

    xpointer = xpointer.trim

    if (xpointer.matches("^[\\w:]+\\s*\\(.*")) {
      val scheme = Pattern.compile("^([\\w+:]+)\\s*(\\(.*)$")
      val matcher = scheme.matcher(xpointer)
      if (matcher.matches) { // scheme(data) ...
        val name = schemeName(matcher.group(1))
        var data = matcher.group(2)
        val dataend = indexOfEnd(data)

        if (dataend < 0) {
          throw XProcException.xcUnparseableXPointer(xpointer)
        }

        var rest = data.substring(dataend)
        data = data.substring(1, dataend - 1) // 1 because we want to skip the initial "("

        data = cleanup(data)

        if (name == _xmlns) {
          parts += new XPointerXmlnsScheme(name, data, readLimit)
        } else if (name == _element) {
          parts += new XPointerElementScheme(name, data, readLimit)
        } else if (name == _xpath) {
          parts += new XPointerXPathScheme(name, data, readLimit)
        } else if (name == _text) {
          parts += new XPointerTextScheme(name, data, readLimit)
        } else if (name == _search) {
          parts += new XPointerTextSearchScheme(name, data, readLimit)
        } else {
          parts += new XPointerScheme(name, data, readLimit)
        }

        if (rest == "") {
          None
        } else {
          Some(rest)
        }
      } else {
        val scheme = Pattern.compile("^([\\w+:]+)\\s*\\(\\)\\s*(.*)$")
        val matcher = scheme.matcher(xpointer)
        if (matcher.matches) { // scheme() ...
          val name = schemeName(matcher.group(1))
          val data = cleanup(matcher.group(2))
          parts += new XPointerScheme(name, data, readLimit)

          val rest = matcher.group(3)
          if (rest == "") {
            None
          } else {
            Some(rest)
          }
        } else {
          throw XProcException.xcUnparseableXPointer(xpointer)
        }
      }
    } else if (xpointer.matches("^[\\w:]+\\s*$")) {
      parts += (new XPointerScheme(_element, xpointer, readLimit))
      None
    } else {
      throw XProcException.xcUnparseableXPointer(xpointer)
    }
  }

  private def schemeName(name: String): QName = {
    if (name.contains(":")) {
      var pos = name.indexOf(":")
      val pfx = name.substring(0, pos)
      val lcl = name.substring(pos + 1)
      pos = parts.size - 1
      while (pos >= 0) {
        val scheme = parts(pos)
        scheme match {
          case xmlns: XPointerXmlnsScheme =>
            val prefix = xmlns.getPrefix
            val uri = xmlns.getURI
            if (pfx == prefix) {
              return new QName(pfx, uri, lcl)
            }
          case _ => ()
        }
        pos -= 1
      }
    } else {
      return new QName("", name)
    }

    throw XProcException.xcXPointerError(s"Scheme name without bound prefix: $name")
  }

  private def indexOfEnd(xdata: String): Int = {
    // Make sure we don't get fooled by ^^, ^(, or ^)
    var data = xdata
    data = data.replaceAll("\\^[\\(\\)]", "xx")

    var depth = 0
    var pos = 0
    var done = false
    while (pos < data.length && !done) {
      val s = data.substring(pos, pos + 1)
      if ("(" == s)  {
        depth += 1
      } else if (")" == s) {
        depth -= 1
      }

      done = ")" == s && depth == 0

      pos += 1
    }

    if (depth != 0) {
      -1
    } else {
      pos
    }
  }

  private def cleanup(data: String) = {
    data.replaceAll("\\^([\\(\\)\\^])", "$1")
  }


  private class XPointerXmlnsScheme(name: QName, data: String, readLimit: Int) extends XPointerScheme(name, data, readLimit) {
    protected var prefix: String = _
    protected var uri: String = _

    private val scheme = Pattern.compile("([\\w:]+)\\s*=\\s*([^=]+)$")
    private val matcher = scheme.matcher(data)
    if (matcher.matches) {
      prefix = matcher.group(1)
      uri = matcher.group(2)
    } else {
      throw XProcException.xcXPointerUnparseableXmlnsScheme(data)
    }

    def getPrefix: String = prefix
    def getURI: String = uri
  }

  private class XPointerElementScheme(name: QName, data: String, readLimit: Int) extends XPointerScheme(name, data, readLimit) {
    override def xpathEquivalent: Option[String] = {
      var xpath = ""
      var data = schemeData
      val pos = data.indexOf("/")
      if (pos < 0) {
        return Some("id('" + data + "')")
      }

      if (pos > 0) {
        xpath = "id('" + data.substring(0, pos) + "')"
        data = data.substring(pos)
      }

      val dscheme = Pattern.compile("^/(\\d+)(.*)$")
      var dmatcher = dscheme.matcher(data)
      while (dmatcher.matches) {
        xpath += "/*[" + dmatcher.group(1) + "]"
        data = dmatcher.group(2)
        dmatcher = dscheme.matcher(data)
      }

      if (data != "") {
        throw XProcException.xcXPointerUnparseableElementScheme(schemeData)
      }

      Some(xpath)
    }
  }

  private class XPointerXPathScheme(name: QName, data: String, readLimit: Int) extends XPointerScheme(name, data, readLimit) {
    override def xpathEquivalent: Option[String] = Some(schemeData)
  }

  private class XPointerTextScheme(name: QName, data: String, readLimit: Int) extends XPointerScheme(name, data, readLimit) {
    override def textEquivalent: Option[String] = Some(schemeData)
  }

  private class XPointerTextSearchScheme(name: QName, data: String, readLimit: Int) extends XPointerScheme(name, data, readLimit) {
    override def textEquivalent: Option[String] = Some(schemeData)
  }
}
