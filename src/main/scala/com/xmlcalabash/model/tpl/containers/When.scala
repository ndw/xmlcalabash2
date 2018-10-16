package com.xmlcalabash.model.tpl.containers

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.tpl.Artifact

class When(override val config: XMLCalabashConfig, override val parent: Option[Artifact]) extends Container(config,parent) {
}
