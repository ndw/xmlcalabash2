package com.xmlcalabash.model.xml.datasource

import com.xmlcalabash.model.util.ParserConfiguration
import com.xmlcalabash.model.xml.Artifact

class DataSource(override val config: ParserConfiguration,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
