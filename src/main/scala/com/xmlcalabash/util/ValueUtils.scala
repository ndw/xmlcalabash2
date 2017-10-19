package com.xmlcalabash.util

import com.jafpl.graph.Location
import net.sf.saxon.s9api.XdmValue

object ValueUtils {
  def singletonStringValue(value: XdmValue, location: Option[Location]): String = {
    if (value.size() == 1) {
      value.itemAt(0).getStringValue
    } else {
      throw new RuntimeException("No sequences.")
    }
  }

  def stringValue(value: XdmValue): String = {
    var s = ""
    val iter = value.iterator()
    while (iter.hasNext) {
      s += iter.next.getStringValue
    }
    s
  }
}
