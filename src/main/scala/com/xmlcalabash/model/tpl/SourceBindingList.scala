package com.xmlcalabash.model.tpl

import com.xmlcalabash.config.XMLCalabashConfig

class SourceBindingList(override val config: XMLCalabashConfig,
                        override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
