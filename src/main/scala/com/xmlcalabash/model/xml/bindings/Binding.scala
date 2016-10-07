package com.xmlcalabash.model.xml.bindings

import com.xmlcalabash.model.xml.Artifact
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/5/16.
  */
class Binding(node: Option[XdmNode], parent: Option[Artifact]) extends Artifact(node, parent) {

}
