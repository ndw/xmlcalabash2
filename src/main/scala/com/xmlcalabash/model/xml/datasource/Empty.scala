package com.xmlcalabash.model.xml.datasource

import com.xmlcalabash.model.xml.Artifact

class Empty(override val parent: Option[Artifact]) extends DataSource(parent) {

}
