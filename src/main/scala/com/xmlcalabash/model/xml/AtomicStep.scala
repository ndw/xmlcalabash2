package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class AtomicStep extends XMLArtifact {
  def this(node: XdmNode, parent: Option[XMLArtifact]) {
    this()
    _xmlname = "atomic-step"
    _node = node
    _parent = parent
    parse(node)
  }

  override def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px(_xmlname))
    tree.addAttribute(XProcConstants.px("type"), _node.getNodeName.toString)
    for (att <- prop) {
      tree.addAttribute(att.name, att.value)
    }
    for (att <- attr) {
      tree.addAttribute(att.name, att.value)
    }
    for (ns <- _nsbindings) {
      tree.addStartElement(XProcConstants.px("ns"))
      tree.addAttribute(XProcConstants._prefix, ns.prefix)
      tree.addAttribute(XProcConstants._uri, ns.uri)
      tree.addEndElement()
    }

    for (child <- children) {
      child.dump(tree)
    }
    tree.addEndElement()
  }

}
