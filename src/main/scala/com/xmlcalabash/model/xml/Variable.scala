package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class Variable(node: Option[XdmNode], parent: Option[XMLArtifact]) extends XMLArtifact(node, parent) {
  override def addDefaultReadablePort(port: Option[InputOrOutput]): Unit = {
    _drp = port
    for (child <- _children) { child.addDefaultReadablePort(port) }
  }

  override def fixBindingsOnIO(): Unit = {
    if (bindings().isEmpty) {
      if (defaultReadablePort.isDefined) {
        val ctx = new XPathContext(None, Some(this))
        ctx._drp = defaultReadablePort
        _children += ctx
      }
    }
    for (child <- _children) { child.fixBindingsOnIO() }
  }
}
