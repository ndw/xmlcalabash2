package com.xmlcalabash.util

import java.io.{ByteArrayInputStream, InputStream}

class ShadowValue private (val contentType: MediaType) {
  private var bytes: Option[Array[Byte]] = None

  protected[xmlcalabash] def this(bytes: Array[Byte], contentType: MediaType) {
    this(contentType)
    this.bytes = Some(bytes)
  }

  def getStream: InputStream = {
    if (bytes.isDefined) {
      new ByteArrayInputStream(bytes.get)
    } else {
      throw new RuntimeException("Attempt to get stream from uninitilized shadow?")
    }
  }
}
