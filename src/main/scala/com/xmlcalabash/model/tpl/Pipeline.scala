package com.xmlcalabash.model.tpl

import com.xmlcalabash.model.tpl.containers.Container
import com.xmlcalabash.model.util.ParserConfiguration

class Pipeline(override val config: ParserConfiguration, override val parent: Option[Artifact])
  extends Container(config,parent) {

}
