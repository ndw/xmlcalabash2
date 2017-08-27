package com.xmlcalabash.model.xml

class Library(override val config: ParserConfiguration,
              override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
