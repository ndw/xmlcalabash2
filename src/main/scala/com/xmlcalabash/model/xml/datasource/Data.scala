package com.xmlcalabash.model.xml.datasource

import com.xmlcalabash.model.util.ParserConfiguration
import com.xmlcalabash.model.xml.Artifact

class Data(override val config: ParserConfiguration,
           override val parent: Option[Artifact]) extends DataSource(config, parent) {

}
