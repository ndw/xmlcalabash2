package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants

/**
  * Created by ndw on 10/7/16.
  */
class ForEachEnd(parent: Option[Artifact]) extends AtomicStep(None, parent) {
  override protected val _stepType = XProcConstants.px_for_each_end

  _xmlname = "for-each-end"

}
