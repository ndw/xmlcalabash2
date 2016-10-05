package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class Input(node: Option[XdmNode], parent: Option[XMLArtifact]) extends InputOrOutput(node, parent) {
  _xmlname = "input"

  override def dumpAdditionalAttributes(tree: TreeWriter): Unit = {
    if (_drp.isDefined) {
      tree.addAttribute(XProcConstants.px("drp"), _drp.get.toString)
    }
  }
}
