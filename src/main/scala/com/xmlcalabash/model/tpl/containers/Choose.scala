package com.xmlcalabash.model.tpl.containers

import com.xmlcalabash.model.tpl.Artifact
import com.xmlcalabash.model.util.ParserConfiguration

class Choose(override val config: ParserConfiguration, override val parent: Option[Artifact]) extends Container(config,parent) {
}
