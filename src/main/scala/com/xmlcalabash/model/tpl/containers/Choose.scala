package com.xmlcalabash.model.tpl.containers

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.tpl.Artifact

class Choose(override val config: XMLCalabash, override val parent: Option[Artifact]) extends Container(config,parent) {
  protected[tpl] def addWhen(cut: When): Unit = {
    _children += cut
  }

  protected[tpl] def addOtherwise(cut: Otherwise): Unit = {
    _children += cut
  }
}
