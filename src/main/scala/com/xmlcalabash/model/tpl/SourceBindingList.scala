package com.xmlcalabash.model.tpl

import com.xmlcalabash.model.util.ParserConfiguration

class SourceBindingList(override val config: ParserConfiguration,
                        override val parent: Option[Artifact]) extends Artifact(config, parent) {

}
