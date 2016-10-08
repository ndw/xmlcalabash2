package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants

/**
  * Created by ndw on 10/7/16.
  */
class IterationBuffer(parent: Option[Artifact]) extends AtomicStep(None, parent) {
  override protected val _stepType = XProcConstants.px_iteration_buffer

  _xmlname = "iteration-buffer"

}
