package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/1/16.
  */
class Import(context: Option[XdmNode], val href: String) extends Artifact(context) {

  def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("import"))
    tree.addAttribute(XProcConstants._href, href)
    tree.addEndElement()
  }
}
