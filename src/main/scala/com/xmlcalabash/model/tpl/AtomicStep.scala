package com.xmlcalabash.model.tpl

import com.xmlcalabash.config.XMLCalabashConfig

class AtomicStep(override val config: XMLCalabashConfig, override val parent: Option[Artifact])
  extends Step(config,parent) {

  protected[tpl] var _name = ""

  def name: String = _name
  protected[tpl] def name_=(str: String): Unit = {
    _name = str
  }
}
