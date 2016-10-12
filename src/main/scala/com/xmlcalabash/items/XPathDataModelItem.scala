package com.xmlcalabash.items

import com.jafpl.items.GenericItem
import net.sf.saxon.s9api.XdmValue

/**
  * Created by ndw on 10/11/16.
  */
class XPathDataModelItem(val value: XdmValue) extends GenericItem {
  override def contentType: String = "application/vnd.xmlcalabash.xdmitem"

  override def toString: String = {
    value.toString
  }
}
