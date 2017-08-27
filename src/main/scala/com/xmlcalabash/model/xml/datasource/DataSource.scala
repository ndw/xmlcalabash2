package com.xmlcalabash.model.xml.datasource

import com.xmlcalabash.model.xml.{Artifact, ParserConfiguration}

class DataSource(override val config: ParserConfiguration,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
