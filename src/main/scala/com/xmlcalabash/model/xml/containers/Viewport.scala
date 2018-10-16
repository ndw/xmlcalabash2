package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.Artifact

class Viewport(override val config: XMLCalabashConfig,
               override val parent: Option[Artifact]) extends Container(config, parent, XProcConstants.p_viewport) {

}
