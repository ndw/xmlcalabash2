package com.xmlcalabash.model.tpl

import com.xmlcalabash.config.XMLCalabash

class VariableBinding(override val config: XMLCalabash,
                      override val parent: Option[Artifact],
                      val name: String,
                      val initializer: String) extends Artifact(config,parent) {

}
