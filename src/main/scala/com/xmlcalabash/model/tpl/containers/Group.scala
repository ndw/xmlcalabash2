package com.xmlcalabash.model.tpl.containers

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.tpl.Artifact

class Group(override val config: XMLCalabash, override val parent: Option[Artifact]) extends Container(config,parent) {
}
