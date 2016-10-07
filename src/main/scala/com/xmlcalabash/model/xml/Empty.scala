package com.xmlcalabash.model.xml

import com.xmlcalabash.model.xml.bindings.Binding
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class Empty(node: Option[XdmNode], parent: Option[Artifact]) extends Binding(node, parent) {
}
