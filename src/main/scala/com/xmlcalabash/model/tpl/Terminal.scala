package com.xmlcalabash.model.tpl

import com.xmlcalabash.model.util.ParserConfiguration

class Terminal(override val config: ParserConfiguration,
               override val parent: Option[Artifact],
               val name: String,
               val text: String) extends Artifact(config, parent) {

}
