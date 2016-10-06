package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class DeclOption(node: Option[XdmNode], parent: Option[Artifact]) extends Artifact(node, parent) {
  override def fixBindingsOnIO(): Unit = {
    if (bindings.isEmpty) {
      if (defaultReadablePort.isDefined) {
        val pipe = new Pipe(None, Some(this))
        pipe._drp = defaultReadablePort
        _children += pipe
      }
    }
    for (child <- _children) { child.fixBindingsOnIO() }
  }

}
