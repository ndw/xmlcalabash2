package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.model.xml.Artifact
import com.xmlcalabash.runtime.XMLCalabashRuntime

class WithDocument(override val config: XMLCalabashRuntime,
                   override val parent: Option[Artifact]) extends ForEach(config, parent)  {
  // Nothing happens here, this gets rewritten at "compile" time into different steps.
}
