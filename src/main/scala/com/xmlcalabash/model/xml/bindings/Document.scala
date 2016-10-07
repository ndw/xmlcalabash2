package com.xmlcalabash.model.xml.bindings

import com.xmlcalabash.model.xml.Artifact
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class Document(node: Option[XdmNode], parent: Option[Artifact]) extends Binding(node, parent) {
}
