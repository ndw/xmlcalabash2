package com.xmlcalabash.model.xml.bindings

import com.xmlcalabash.model.xml.Artifact
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/1/16.
  */
abstract class Binding(override val context: Option[XdmNode]) extends Artifact(context: Option[XdmNode]) {
}
