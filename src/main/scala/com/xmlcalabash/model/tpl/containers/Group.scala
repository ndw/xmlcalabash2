package com.xmlcalabash.model.tpl.containers

import com.xmlcalabash.model.tpl.{Artifact, Cut, Step}
import com.xmlcalabash.model.util.ParserConfiguration

import scala.collection.mutable.ListBuffer

class Group(override val config: ParserConfiguration, override val parent: Option[Artifact]) extends Container(config,parent) {
}
