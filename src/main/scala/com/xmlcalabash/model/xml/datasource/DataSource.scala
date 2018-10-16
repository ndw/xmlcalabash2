package com.xmlcalabash.model.xml.datasource

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.xml.Artifact

class DataSource(override val config: XMLCalabashConfig,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
