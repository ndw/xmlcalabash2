package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.decl.StepDecl
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/5/16.
  */
class ExprStep(parent: Artifact) extends Step(None, Some(parent)) {
  _xmlname = "expression-step"

}
