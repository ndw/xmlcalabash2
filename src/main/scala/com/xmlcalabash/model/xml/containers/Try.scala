package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.xml.Artifact

class Try(override val config: XMLCalabash,
          override val parent: Option[Artifact]) extends Container(config, parent) {

}
