package com.xmlcalabash.model.xml.datasource

import com.xmlcalabash.model.xml.{Artifact, ParserConfiguration}

class Data(override val config: ParserConfiguration,
           override val parent: Option[Artifact]) extends DataSource(config, parent) {

}
