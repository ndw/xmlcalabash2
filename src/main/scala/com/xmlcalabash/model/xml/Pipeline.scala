package com.xmlcalabash.model.xml

import com.xmlcalabash.runtime.XMLCalabashRuntime

class Pipeline(override val config: XMLCalabashRuntime,
               override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
