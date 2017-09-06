package com.xmlcalabash.model.xml.datasource

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.xml.Artifact

class Data(override val config: XMLCalabash,
           override val parent: Option[Artifact]) extends DataSource(config, parent) {

}
