package com.xmlcalabash.model.tpl

import com.xmlcalabash.config.XMLCalabashConfig

class VariableBinding(override val config: XMLCalabashConfig,
                      override val parent: Option[Artifact],
                      val name: String,
                      val initializer: String) extends Artifact(config,parent) {

}
