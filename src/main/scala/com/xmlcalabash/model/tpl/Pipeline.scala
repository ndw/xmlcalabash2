package com.xmlcalabash.model.tpl

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.tpl.containers.Container

class Pipeline(override val config: XMLCalabashConfig, override val parent: Option[Artifact])
  extends Container(config,parent) {

}
