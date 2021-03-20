package com.xmlcalabash.util

import java.io.BufferedReader
import java.util.regex.{Matcher, Pattern}

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import net.sf.saxon.s9api.{QName, XdmNode}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer

// This is a naive translation from Java. Could do better.

class XPointerScheme(val schemeName: QName, val schemeData: String, val readLimit: Int) {
  protected var logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val rangeRE = Pattern.compile("^.*?=(\\d*)?(,(\\d*)?)?$")
  private val lengthRE = Pattern.compile("^length=(\\d+)(,[^;]*)?(.*)$")
  private val leadingWhitespaceRE = Pattern.compile("^(\\s*)(\\S.*)$")
  private val INCLUDE_MATCH = 0
  private val EXCLUDE_MATCH = 1
  private val TRIM = 2

  private var sp = -1
  private var ep = -1
  private var chars = false
  private var cp = -1
  private var lp = -1

  def xpathEquivalent: Option[String] = None
  def textEquivalent: Option[String] = None

  def selectNodes(runtime: XMLCalabashRuntime, doc: XdmNode, nsBindings: Map[String,String]): List[XdmNode] = {
    val select = xpathEquivalent
    if (select.isEmpty) {
      throw XProcException.xcXPointerError(s"XPointer cannot be used to select nodes: $schemeName($schemeData)")
    }

    val xcomp = runtime.processor.newXPathCompiler()
    for ((pfx,uri) <- nsBindings) {
      xcomp.declareNamespace(pfx, uri)
    }

    val xexec = xcomp.compile(select.get)
    val selector = xexec.load()
    selector.setContextItem(doc)

    val selectedNodes = ListBuffer.empty[XdmNode]
    val iter = selector.iterator()
    while (iter.hasNext) {
      val item = iter.next()
      item match {
        case node: XdmNode =>
          selectedNodes += node
        case _ =>
          throw XProcException.xcXPointerError(s"XPointer matched non-node item: $schemeName($schemeData)")
      }
    }

    selectedNodes.toList
  }

  def selectText(rd: BufferedReader, contentLength: Int): String = {
    val select = textEquivalent
    if (select.isEmpty) {
      throw XProcException.xcXPointerError(s"XPointer cannot be used to select text: $schemeName($schemeData)")
    }

    // RFC 5147:
    // text-fragment   =  text-scheme 0*( ";" integrity-check )
    // text-scheme     =  ( char-scheme / line-scheme )
    // char-scheme     =  "char=" ( position / range )
    // line-scheme     =  "line=" ( position / range )
    // integrity-check =  ( length-scheme / md5-scheme )
    //                      [ "," mime-charset ]
    // position        =  number
    // range           =  ( position "," [ position ] ) / ( "," position )
    // number          =  1*( DIGIT )
    // length-scheme   =  "length=" number
    // md5-scheme      =  "md5=" md5-value
    // md5-value       =  32HEXDIG

    var data = ""
    try {
      rd.mark(readLimit)

      val parts = select.get.split("\\s*;\\s*");
      for (pos <- 1 to parts.length) {
        // start at 1 because we want to skip the scheme
        val check = parts(pos)
        val matcher = lengthRE.matcher(check)
        if (contentLength >= 0 && matcher.matches()) {
          val checklen = matcher.group(1).toInt
          if (checklen != contentLength) {
            throw new IllegalArgumentException("Integrity check failed: " + checklen + " != " + contentLength)
          }
        }
      }
      val selected = parts(0).trim()

      sp = -1;
      ep = Int.MaxValue;
      cp = 0;
      lp = 0;

      // FIXME: Isn't there a better way to do this?
      val matcher = rangeRE.matcher(selected)
      if (matcher.matches) {
        var r = matcher.group(1)
        if (r != null && r != "") {
          sp = r.toInt
        }
        r = matcher.group(3)
        if (r != null && r != "") {
          ep = r.toInt
        }
      }

      if (selected.startsWith("char=")) {
        chars = true
      } else if (selected.startsWith("line=")) {
        chars = false
      } else {
        throw XProcException.xcUnparseableXPointer(s"$schemeName($schemeData)")
      }

      var line = rd.readLine()
      while (line != null) {
        if (chars) {
          data += selectChars(line)
        } else {
          data += selectLines(line)
        }
        line = rd.readLine()
      }
    } finally {
      try {
        rd.reset()
      } catch {
        case _: Exception => ()
      }
    }

    data
  }

  private def selectChars(inline: String): String = {
    var line = inline
    var data = "";
    val endcp = cp + line.length()+1;

    if (cp < sp && endcp > sp) {
      line = line.substring((sp - cp));
      cp = sp;
    }

    if (cp >= sp && cp < ep) {
      val rest = ep - cp;
      if (rest > line.length()) {
        data = line + "\n";
        cp = endcp;
      } else {
        data += line.substring(0, rest);
        cp += rest;
      }
    }

    cp = endcp;

    data;
  }

  private def selectLines(line: String): String = {
    var data = "";
    if (lp >= sp && lp < ep) {
      data = line + "\n";
    }
    lp += 1;
    data;
  }

  def selectSearchText(rd: BufferedReader, contentLength: Int): String = {
    val select = textEquivalent
    if (select.isEmpty) {
      throw XProcException.xcXPointerError(s"XPointer cannot be used to select text: $schemeName($schemeData)")
    }

    // search=(digit)/string/opt,(digit)/string/opt;integrity
    // Where start and end can be enclosed in character
    // and the options for start opt are "from", "after", or "trim"
    // and the options for end opt are "to", "before", or "trim"

    // Yes, this is probably all horribly inefficient...

    var matcher: Matcher = null
    val origSelect = select.get
    var startSearch = Option.empty[String]
    var startOpt = INCLUDE_MATCH
    var startCount = 1
    var endSearch = Option.empty[String]
    var endOpt = INCLUDE_MATCH
    var endCount = 1
    var found = false
    var strip = false
    var stripWS = Int.MaxValue

    var selected = select.get.substring(7).trim();
    if (selected == "") {
      malformedSearch("at least one of start/end required", origSelect);
    }

    var skip = ""
    var ch = selected.charAt(0)
    if (ch == ',') {
      selected = selected.substring(1)
    } else {
      while (Character.isDigit(ch)) {
        skip += ch
        selected = selected.substring(1)
        if (selected == "") {
          malformedSearch("start must specify a search string", origSelect)
        }
        ch = selected.charAt(0)
      }

      if (skip != "") {
        startCount = skip.toInt
      }

      selected = selected.substring(1)
      var pos = selected.indexOf(ch)
      if (pos < 0) {
        malformedSearch("unterminated start string", origSelect)
      }
      startSearch = Some(selected.substring(0, pos))
      selected = selected.substring(pos+1).trim()

      if (selected.startsWith("trim")) {
        startOpt = TRIM;
        selected = selected.substring(4).trim();
      } else if (selected.startsWith("from")) {
        startOpt = INCLUDE_MATCH;
        selected = selected.substring(4).trim();
      } else if (selected.startsWith("after")) {
        startOpt = EXCLUDE_MATCH;
        selected = selected.substring(5).trim();
      } else if (selected == "" || selected.startsWith(",")) {
        // ok
      } else {
        malformedSearch("invalid start option", origSelect);
      }
    }

    if (selected.startsWith(",")) {
      selected = selected.substring(1);
    }

    if (selected != "") {
      skip = ""
      ch = selected.charAt(0)
      while (Character.isDigit(ch)) {
        skip = skip + ch
        selected = selected.substring(1)
        if ("" == selected) {
          malformedSearch("end must specify a search string", origSelect)
        }
        ch = selected.charAt(0)
      }

      if (skip != "") {
        endCount = skip.toInt
      }

      selected = selected.substring(1)
      val pos = selected.indexOf(ch)
      if (pos < 0) {
        malformedSearch("unterminated end string", origSelect)
      }
      endSearch = Some(selected.substring(0, pos))
      selected = selected.substring(pos + 1).trim

      if (selected.startsWith("trim")) {
        endOpt = TRIM
        selected = selected.substring(4).trim
      }
      else if (selected.startsWith("to")) {
        endOpt = INCLUDE_MATCH
        selected = selected.substring(2).trim
      }
      else if (selected.startsWith("before")) {
        endOpt = EXCLUDE_MATCH
        selected = selected.substring(6).trim
      }
    }

    if (selected.startsWith(";")) {
      selected = selected.substring(1).trim
    }

    if (selected.startsWith("strip")) {
      strip = true
      selected = selected.substring(5).trim
      if (selected.startsWith(";")) {
        selected = selected.substring(1).trim
      }
    }

    logger.trace("XPointer search scheme: search='" + startSearch + "';" + startOpt + ",'" + endSearch + "';" + endOpt)
    val data = new StringBuilder()
    try {
      rd.mark(readLimit)

      matcher = lengthRE.matcher(selected)
      if (matcher.matches) {
        val checklen = matcher.group(1).toInt
        val charset = matcher.group(2)
        selected = matcher.group(3)
        if (contentLength >= 0) if (checklen != contentLength) {
          throw new IllegalArgumentException("Integrity check failed: " + checklen + " != " + contentLength)
        }
        if (selected.startsWith(";")) {
          selected = selected.substring(1).trim
        }
        if (selected.startsWith("strip")) {
          strip = true
          selected = selected.substring(5).trim
        }
      }

      if (selected != "") {
        malformedSearch("unexpected characters at end", origSelect)
      }

      val lines = ListBuffer.empty[String]
      var finished = false
      var output = false
      var line = rd.readLine()
      while (!finished && line != null) {
        if (output && endSearch.isDefined && line.contains(endSearch.get)) {
          if (endCount == 1) {
            output = false
            finished = true
            if (endOpt == INCLUDE_MATCH) {
              lines += line
            }
          }
          endCount -= 1
        }

        if (output) {
          lines += line
        }

        if (startSearch.isEmpty || line.contains(startSearch.get)) {
          found = true
          if (startCount == 1) {
            output = true
            if (startOpt == INCLUDE_MATCH) {
              lines += line
            }
          }
          startCount -= 1
        }

        line = rd.readLine()
      }

      if (!found) {
        throw XProcException.xcXPointerNotFound(origSelect)
      }

      if (lines.nonEmpty) {
        if (strip && stripWS > 0) {
          for (line <- lines) {
            matcher = leadingWhitespaceRE.matcher(line)
            if (matcher.matches) {
              val wslen = matcher.group(1).length
              if (wslen < stripWS) {
                stripWS = wslen
              }
            }
          }
        }

        while (lines.nonEmpty && startOpt == TRIM && lines.head.trim == "") {
          lines.remove(0)
        }

        while (lines.nonEmpty && endOpt == TRIM && lines.last.trim == "") {
          lines.remove(lines.size - 1)
        }
      }

      for (line <- lines) {
        if (strip && stripWS > 0 && line.length >= stripWS) {
          data.append(line.substring(stripWS) + "\n")
        } else {
          data.append(line + "\n")
        }
      }
    } finally {
      try {
        rd.reset()
      } catch {
        case _: Exception => ()
      }
    }

    data.toString()
  }

  private def malformedSearch(select: String, msg: String): Unit = {
    throw XProcException.xcXPointerMalformedSearch(select, msg);
  }
}
