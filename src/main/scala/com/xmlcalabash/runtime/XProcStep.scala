package com.xmlcalabash.runtime

import com.jafpl.runtime.Step
import com.jafpl.util.SourceLocation
import com.xmlcalabash.core.XProcEngine

/**
  * Created by ndw on 10/11/16.
  */
trait XProcStep extends Step {
  def engine: XProcEngine
  def engine_=(engine: XProcEngine)
  def label_=(label: String)
  def location_=(location: SourceLocation)

}
