package com.xmlcalabash.model.tpl

import com.xmlcalabash.config.XMLCalabash

class Terminal(override val config: XMLCalabash,
               override val parent: Option[Artifact],
               val name: String,
               val text: String) extends Artifact(config, parent) {

}
