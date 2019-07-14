package com.xmlcalabash.runtime.params

import com.jafpl.graph.BindingParams
import net.sf.saxon.s9api.{QName, XdmValue}

object XPathBindingParams {
  private val _empty = new XPathBindingParams(Map.empty[QName,XdmValue])
  def EMPTY: XPathBindingParams = _empty
}

class XPathBindingParams(val statics: Map[QName, XdmValue]) extends BindingParams {

}
