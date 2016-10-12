package com.xmlcalabash.runtime

import com.jafpl.items.GenericItem
import com.xmlcalabash.items.XPathDataModelItem
import net.sf.saxon.s9api.{XdmNode, XdmValue}

/**
  * Created by ndw on 10/11/16.
  */
class ContentTypeManager {
  def convert(item: GenericItem, contentTypes: List[String]): Option[GenericItem] = {
    None
  }

  def convertToXdm(item: GenericItem): Option[XdmValue] = {
    item match {
      case xdm: XPathDataModelItem => Some(xdm.value)
      case _ => None
    }
  }

  def convertToXdmNode(item: GenericItem): Option[XdmNode] = {
    item match {
      case xdm: XPathDataModelItem =>
        xdm.value match {
          case node: XdmNode => Some(node)
          case _ => None
        }
      case _ => None
    }
  }
}
