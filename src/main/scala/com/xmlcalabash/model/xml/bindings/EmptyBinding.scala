package com.xmlcalabash.model.xml.bindings

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/1/16.
  */
class EmptyBinding(override val context: Option[XdmNode]) extends Binding(context: Option[XdmNode]) {

  def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("empty"))
    tree.addEndElement()
  }
}
