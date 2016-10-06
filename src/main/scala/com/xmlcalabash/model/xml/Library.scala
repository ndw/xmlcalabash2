package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcEngine
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class Library(node: Option[XdmNode], parent: Option[Artifact]) extends PipelineDocument(node, parent) {
}
