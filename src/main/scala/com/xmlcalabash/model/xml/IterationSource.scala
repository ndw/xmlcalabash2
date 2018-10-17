package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.runtime.XMLCalabashRuntime

class IterationSource(override val config: XMLCalabashRuntime,
                      override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
