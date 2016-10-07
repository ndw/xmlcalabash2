package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/7/16.
  */
class ForEachBegin(parent: Option[Artifact]) extends AtomicStep(None, parent) {
  override protected val _stepType = XProcConstants.px_for_each_begin

  _xmlname = "for-each-begin"

}
