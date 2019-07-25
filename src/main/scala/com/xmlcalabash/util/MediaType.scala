package com.xmlcalabash.util

import com.xmlcalabash.exceptions.XProcException

import scala.collection.mutable.ListBuffer

// This isn't trying very hard to be strict about the rules
// N.B. This class accepts "*" for type and subtype because it's used for matching

object MediaType {
  val OCTET_STREAM = new MediaType("application", "octet-stream")
  val TEXT = new MediaType("text", "plain")
  val XML  = new MediaType("application", "xml")
  val JSON = new MediaType("application", "json")
  val HTML = new MediaType("text", "html")

  val MATCH_XML: Array[MediaType] = Array(
    MediaType.parse("application/xml"),
    MediaType.parse("text/xml"),
    MediaType.parse("*/*+xml"),
    MediaType.parse("-application/xhtml+xml"))

  val MATCH_HTML: Array[MediaType] = Array(
    MediaType.parse("text/html"),
    MediaType.parse("application/xhtml+xml")
  )

  val MATCH_TEXT: Array[MediaType] = Array(
    MediaType.parse("text/*"),
    MediaType.parse("-text/html"),
    MediaType.parse("-text/xml")
  )

  val MATCH_JSON: Array[MediaType] = Array(
    MediaType.parse("application/json")
  )

  val MATCH_ANY: Array[MediaType] = Array(
    MediaType.parse("*/*")
  )

  def parse(mtype: Option[String]): Option[MediaType] = {
    if (mtype.isDefined) {
      Some(parse(mtype.get))
    } else {
      None
    }
  }

  def parse(mtype: String): MediaType = {
    // [-]type/subtype; name1=val1; name2=val2
    var pos = mtype.indexOf("/")
    if (pos <= 0) {
      throw XProcException.xcUnrecognizedContentType(mtype, None)
    }
    var mediaType = mtype.substring(0, pos).trim
    var rest = mtype.substring(pos + 1)
    val plist = ListBuffer.empty[String]

    var inclusive = true
    if (mediaType.startsWith("-")) {
      inclusive = false
      mediaType = mediaType.substring(1)
    }

    // This is a bit convoluted because of the way 'rest' is reused.
    // There was a bug and this was the easiest fix. #hack

    var params = ""
    pos = rest.indexOf(";")
    if (pos >= 0) {
      params = rest.substring(pos+1)
      rest = rest.substring(0, pos).trim
    }

    var mediaSubtype = rest
    var suffix = Option.empty[String]

    if (mediaSubtype.contains("+")) {
      pos = mediaSubtype.indexOf("+")
      suffix = Some(mediaSubtype.substring(pos+1).trim)
      mediaSubtype = mediaSubtype.substring(0, pos).trim
    }

    if (params == "") {
      new MediaType(mediaType, mediaSubtype, suffix, inclusive, None)
    } else {
      rest = params
      pos = rest.indexOf(";")
      while (pos >= 0) {
        val param = rest.substring(0, pos).trim
        rest = rest.substring(pos+1)
        if (param != "") {
          plist.append(param)
        }
        pos = rest.indexOf(";")
      }
      if (rest.trim != "") {
        plist.append(rest.trim)
      }
      new MediaType(mediaType, mediaSubtype, suffix, inclusive, Some(plist.toArray))
    }
  }

  def parseList(ctypes: String): ListBuffer[MediaType] = {
    val contentTypes = ListBuffer.empty[MediaType]
    for (ctype <- ctypes.split("\\s+")) {
      ctype match {
        case "xml"  => contentTypes ++= MATCH_XML
        case "html" => contentTypes ++= MATCH_HTML
        case "text" => contentTypes ++= MATCH_TEXT
        case "json" => contentTypes ++= MATCH_JSON
        case "any"  => contentTypes ++= MATCH_ANY
        case _      => contentTypes += MediaType.parse(ctype)
      }
    }
    contentTypes
  }
}

class MediaType(val mediaType: String, val mediaSubtype: String, val suffix: Option[String], val inclusive: Boolean, val param: Option[Array[String]]) {
  def this(mediaType: String, mediaSubtype: String) {
    this(mediaType, mediaSubtype, None, true, None)
  }

  def this(mediaType: String, mediaSubtype: String, suffix: String) {
    this(mediaType, mediaSubtype, Some(suffix), true, None)
  }

  def classification: MediaType = {
    if (xmlContentType) {
      MediaType.XML
    } else if (htmlContentType) {
      MediaType.HTML
    } else if (textContentType) {
      MediaType.TEXT
    } else if (jsonContentType) {
      MediaType.JSON
    } else {
      MediaType.OCTET_STREAM
    }
  }

  def textContentType: Boolean = {
    val mtype = matchingMediaType(MediaType.MATCH_TEXT)
    mtype.isDefined && mtype.get.inclusive
  }

  def xmlContentType: Boolean = {
    val mtype = matchingMediaType(MediaType.MATCH_XML)
    mtype.isDefined && mtype.get.inclusive
  }

  def jsonContentType: Boolean = {
    val mtype = matchingMediaType(MediaType.MATCH_JSON)
    mtype.isDefined && mtype.get.inclusive
  }

  def htmlContentType: Boolean = {
    val mtype = matchingMediaType(MediaType.MATCH_HTML)
    mtype.isDefined && mtype.get.inclusive
  }

  def anyContentType: Boolean = true

  def markupContentType: Boolean = {
    xmlContentType || htmlContentType
  }

  def matchingMediaType(mtypes: Array[MediaType]): Option[MediaType] = {
    var matching = Option.empty[MediaType]
    for (mtype <- mtypes) {
      //println(s"$this $mtype ${matches(mtype)}")
      if (matches(mtype)) {
        matching = Some(mtype)
      }
    }
    matching
  }

  def matches(mtype: MediaType): Boolean = {
    if (mtype.mediaType == "application" && mtype.mediaSubtype == "octet-stream") {
      return true
    }

    var mmatch = mediaType == mtype.mediaType || mtype.mediaType == "*"
    mmatch = mmatch && (mediaSubtype == mtype.mediaSubtype || mtype.mediaSubtype == "*")
    if (suffix.isDefined && mtype.suffix.isDefined) {
      mmatch = mmatch && suffix.get == mtype.suffix.get
    }

    // application/xml should match */*
    // but text/plain shouldn't match */*+xml

    // This special rule seems necessary but I can't really justify it
    if (mmatch && mtype.mediaType == "*" && mtype.mediaSubtype == "*" && mtype.suffix.isDefined) {
      mmatch = mmatch && suffix.isDefined
    }

    mmatch
  }

  def charset: Option[String] = {
    if (param.isDefined) {
      for (param <- param.get) {
        if (param.startsWith("charset=")) {
          return Some(param.substring(8))
        }
      }
    }
    None
  }

  override def toString: String = {
    var ctype = if (inclusive) {
      ""
    } else {
      "-"
    }
    ctype += mediaType + "/" + mediaSubtype
    if (suffix.isDefined) {
      ctype = ctype + "+" + suffix.get
    }
    if (param.isDefined) {
      for (param <- param.get) {
        ctype = ctype + ";" + param
      }
    }
    ctype
  }
}
