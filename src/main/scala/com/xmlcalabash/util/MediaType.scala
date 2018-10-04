package com.xmlcalabash.util

import com.xmlcalabash.exceptions.XProcException

import scala.collection.mutable.ListBuffer

// This isn't trying very hard to be strict about the rules
// N.B. This class accepts "*" for type and subtype because it's used for matching

object MediaType {
  def ANY = new MediaType("*", "*")
  def OCTET_STREAM = new MediaType("application", "octet-stream")
  def TEXT = new MediaType("text", "plain")
  def XML = new MediaType("application", "xml")

  def parse(mtype: Option[String]): Option[MediaType] = {
    if (mtype.isDefined) {
      Some(parse(mtype.get))
    } else {
      None
    }
  }

  def parse(mtype: String): MediaType = {
    // type/subtype; name1=val1; name2=val2
    var pos = mtype.indexOf("/")
    if (pos <= 0) {
      throw XProcException.xsBadTypeValue("content-types", "content type")
    }
    val mediaType = mtype.substring(0, pos).trim
    var rest = mtype.substring(pos + 1)
    val plist = ListBuffer.empty[String]

    pos = rest.indexOf(";")
    if (pos < 0) {
      new MediaType(mediaType, rest.trim)
    } else {
      var mediaSubtype = rest.substring(0, pos).trim
      var suffix = Option.empty[String]

      if (mediaSubtype.contains("+")) {
        pos = mediaSubtype.indexOf("+")
        suffix = Some(mediaSubtype.substring(pos+1).trim)
        mediaSubtype = mediaSubtype.substring(0, pos).trim
      }

      rest = rest.substring(pos+1)
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
      new MediaType(mediaType, mediaSubtype, suffix, Some(plist.toArray))
    }
  }

  def parseList(ctypes: String): ListBuffer[MediaType] = {
    val contentTypes = ListBuffer.empty[MediaType]
    for (ctype <- ctypes.split("\\s+")) {
      val mtype = MediaType.parse(ctype)
      contentTypes += mtype
    }
    contentTypes
  }
}

class MediaType(val mediaType: String, val mediaSubtype: String, val suffix: Option[String], val param: Option[Array[String]]) {
  def this(mediaType: String, mediaSubtype: String) {
    this(mediaType, mediaSubtype, None, None)
  }

  def this(mediaType: String, mediaSubtype: String, suffix: String) {
    this(mediaType, mediaSubtype, Some(suffix), None)
  }

  def textContentType: Boolean = {
    mediaType == "text"
  }

  def xmlContentType: Boolean = {
    ((mediaType == "application" && mediaSubtype == "xml")
      || (mediaType == "text" && mediaSubtype == "xml")
      || (suffix.isDefined && suffix.get == "xml"))
  }

  def jsonContentType: Boolean = {
    (mediaType == "application" || mediaType == "text") && (mediaSubtype == "json")
  }

  def htmlContentType: Boolean = {
    (mediaType == "text" && mediaSubtype == "html")
  }

  def markupContentType: Boolean = {
    xmlContentType || jsonContentType || htmlContentType
  }

  def matches(mtype: MediaType): Boolean = {
    if (xmlContentType && mtype.xmlContentType) {
      return true
    }

    if (mtype.mediaType == "*") {
      return true
    }

    if (mediaType != mtype.mediaType) {
      return false
    }

    if (mtype.mediaSubtype == "*") {
      return true
    }

    mediaSubtype == mtype.mediaSubtype
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
    var ctype = mediaType + "/" + mediaSubtype
    if (suffix.isDefined) {
      ctype = ctype + "+" + suffix
    }
    if (param.isDefined) {
      for (param <- param.get) {
        ctype = ctype + ";" + param
      }
    }
    ctype
  }
}
