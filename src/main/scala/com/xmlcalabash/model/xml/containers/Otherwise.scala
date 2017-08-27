package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.model.util.ParserConfiguration
import com.xmlcalabash.model.xml.Artifact

class Otherwise(override val config: ParserConfiguration,
                override val parent: Option[Artifact]) extends Container(config, parent) {

}
