package com.xmlcalabash.model.xml.util

import com.xmlcalabash.model.xml.{Artifact, CompoundStep}
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/15/16.
  */
class WhenOrOtherwise(node: Option[XdmNode], parent: Option[Artifact]) extends CompoundStep(node, parent) {

}
