package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class IterationSource(node: Option[XdmNode], parent: Option[Artifact]) extends InputOrOutput(node, parent) {
}
