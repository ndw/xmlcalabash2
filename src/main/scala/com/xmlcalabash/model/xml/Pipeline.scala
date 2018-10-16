package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig

class Pipeline(override val config: XMLCalabashConfig,
               override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
