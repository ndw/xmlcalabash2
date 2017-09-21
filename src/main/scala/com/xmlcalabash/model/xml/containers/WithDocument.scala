package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.xml.Artifact

class WithDocument(override val config: XMLCalabash,
                   override val parent: Option[Artifact]) extends ForEach(config, parent)  {
  // Nothing happens here, this gets rewritten at "compile" time into different steps.
}
