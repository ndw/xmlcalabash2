package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.model.xml.{Artifact, ParserConfiguration}

class Catch(override val config: ParserConfiguration,
            override val parent: Option[Artifact]) extends Container(config, parent) {

}
