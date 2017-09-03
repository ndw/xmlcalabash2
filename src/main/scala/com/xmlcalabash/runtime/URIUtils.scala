package com.xmlcalabash.runtime

import java.net.URI

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
}
