package com.xmlcalabash.util

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmValue}

class SerializationOptions(config: XMLCalabash, opts: Map[QName, XdmAtomicValue]) {
  def this(config: XMLCalabash) = {
    this(config, Map())
  }

  // FIXME: Defaults should come from config!

  def method: String = {
    opts.getOrElse(XProcConstants._method, "xml").toString
  }

  def version: String = {
    opts.getOrElse(XProcConstants._version, "1.0").toString
  }

}
