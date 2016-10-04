package com.xmlcalabash.model.xml.bindings

import java.net.URI

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.{QName, XdmNode}

/**
  * Created by ndw on 10/1/16.
  */
class DocumentBinding(override val context: Option[XdmNode], val href: URI) extends Binding(context: Option[XdmNode]) {
  var _overrideContentType: Option[String] = None

  def overrideContentType = _overrideContentType
  def overrideContentType_=(value: Option[String]): Unit = {
    _overrideContentType = value
  }

  def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("document"))
    tree.addAttribute(XProcConstants._href, href.toASCIIString)
    if (overrideContentType.isDefined) {
      tree.addAttribute(new QName("", "override-content-type"), overrideContentType.get)
    }
    tree.addEndElement()
  }
}
