package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class AtomicStep(node: Option[XdmNode], parent: Option[XMLArtifact]) extends XMLArtifact(node, parent) {
  override def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px(_xmlname))
    if (node.isDefined) {
      tree.addAttribute(XProcConstants.px("type"), node.get.getNodeName.toString)
    }
    for (att <- _prop) {
      tree.addAttribute(att.name, att.value)
    }
    for (att <- _attr) {
      tree.addAttribute(att.name, att.value)
    }
    for (ns <- _nsbindings) {
      tree.addStartElement(XProcConstants.px("ns"))
      tree.addAttribute(XProcConstants._prefix, ns.prefix)
      tree.addAttribute(XProcConstants._uri, ns.uri)
      tree.addEndElement()
    }

    for (child <- _children) {
      child.dump(tree)
    }
    tree.addEndElement()
  }

}
