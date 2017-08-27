package com.xmlcalabash.model.xml

import com.xmlcalabash.model.util.ParserConfiguration

class Library(override val config: ParserConfiguration,
              override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
