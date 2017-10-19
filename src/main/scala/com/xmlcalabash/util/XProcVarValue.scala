package com.xmlcalabash.util

import com.xmlcalabash.runtime.ExpressionContext
import net.sf.saxon.s9api.XdmValue

class XProcVarValue(val value: XdmValue, val context: ExpressionContext) {
  def getStringValue: String = {
    val iter = value.iterator()
    var s = ""
    while (iter.hasNext) {
      s += iter.next.getStringValue
    }
    s
  }
}
