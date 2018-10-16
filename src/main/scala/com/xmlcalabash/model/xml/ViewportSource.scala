package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig

class ViewportSource(override val config: XMLCalabashConfig,
                     override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
