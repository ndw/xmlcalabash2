package com.xmlcalabash.model.tpl

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.tpl.containers.Container

class Pipeline(override val config: XMLCalabash, override val parent: Option[Artifact])
  extends Container(config,parent) {

}
