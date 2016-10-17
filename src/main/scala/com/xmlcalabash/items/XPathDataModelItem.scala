package com.xmlcalabash.items

import com.jafpl.items.GenericItem
import net.sf.saxon.s9api.XdmValue

/**
  * Created by ndw on 10/11/16.
  */
class XPathDataModelItem(val value: XdmValue, val inscopeNamespaces: Map[String,String]) extends GenericItem {
  def this(value: XdmValue) {
    this(value, Map.empty[String,String])
  }

  override def contentType: String = "application/vnd.xmlcalabash.xdmitem"

  override def toString: String = {
    value.toString
  }
}
