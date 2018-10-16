package com.xmlcalabash.model.tpl

import com.xmlcalabash.config.XMLCalabashConfig

class Terminal(override val config: XMLCalabashConfig,
               override val parent: Option[Artifact],
               val name: String,
               val text: String) extends Artifact(config, parent) {

}
