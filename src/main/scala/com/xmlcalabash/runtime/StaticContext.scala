package com.xmlcalabash.runtime

import java.net.URI

class StaticContext {
  private var _baseURI = Option.empty[URI]

  def baseURI: Option[URI] = _baseURI
  protected[xmlcalabash] def baseURI_=(uri: Option[URI]): Unit = {
    _baseURI = uri
  }
}
