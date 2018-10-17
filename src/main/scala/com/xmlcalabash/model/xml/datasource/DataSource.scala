package com.xmlcalabash.model.xml.datasource

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.xml.Artifact
import com.xmlcalabash.runtime.XMLCalabashRuntime

class DataSource(override val config: XMLCalabashRuntime,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
