package com.xmlcalabash.runtime

import com.jafpl.runtime.DefaultStep
import com.xmlcalabash.core.XProcEngine

/**
  * Created by ndw on 10/11/16.
  */
class DefaultXProcStep extends DefaultStep with XProcStep {
  private var _engine: XProcEngine = _

  override def engine: XProcEngine = _engine

  override def engine_=(engine: XProcEngine): Unit = {
    _engine = engine
  }
}
