package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabash

class Log(override val config: XMLCalabash,
          override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
