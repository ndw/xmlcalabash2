package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.model.xml.Artifact

class Group(override val parent: Option[Artifact]) extends Container(parent) {

}
