package com.xmlcalabash.util

import java.io.File
import java.net.URI

import com.xmlcalabash.exceptions.XProcException

object URIUtils {
  def homeAsURI: URI = {
    dirAsURI(System.getProperty("user.home"))
  }

  def cwdAsURI: URI = {
    dirAsURI(System.getProperty("user.dir"))
  }

  def dirAsURI(dir: String): URI = {
    var path = encode(dir)
    if (!path.endsWith("/")) {
      path += "/"
    }
    if (!path.startsWith("/")) {
      path = "/" + path
    }
    new URI("file:" + path)
  }

  def encode(uri: URI): String = {
    encode(uri.toASCIIString)
  }

  def encode(src: String): String = {
    val genDelims = ":/?#[]@"
    val subDelims = "!$&'()*+,;="
    val unreserved = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~"
    val okChars = genDelims + subDelims + unreserved + "%"

    // don't double-escape %-escaped chars!
    // FIXME: This should be more general, but Windows seems to be the only problem
    // and I'm oto lazy to look up how to dyanmically escape "\"

    val filesep = System.getProperty("file.separator")
    val adjsrc = if ("\\" == filesep) {
      src.replaceAll("\\\\", "/")
    }  else {
      src
    }

    var encoded = ""
    val bytes = src.getBytes("UTF-8")
    for (ch <- bytes) {
      if (okChars.indexOf(ch) >= 0) {
        encoded += ch.toChar
      } else {
        encoded += "%%%02X".format(ch)
      }
    }

    encoded
  }

  def toFile(uri: URI): File = {
    if (!"file".equalsIgnoreCase(uri.getScheme)) {
      throw new IllegalStateException("Expecting a file URI: " + uri.toASCIIString)
    }
    if (uri.getAuthority != null && uri.getAuthority.length > 0) {
      new File("//" + uri.getAuthority + uri.getPath)
    }
    else {
      new File(uri.getPath)
    }
  }
}
