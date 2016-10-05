package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class Output(node: Option[XdmNode], parent: Option[XMLArtifact]) extends InputOrOutput(node, parent) {
  _xmlname = "output"

  override def fixBindingsOnIO(): Unit = {
    if (parent.isDefined && parent.get.isInstanceOf[CompoundStep]) {
      super.fixBindingsOnIO()
    }
  }
}
