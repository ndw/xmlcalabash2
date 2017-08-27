package com.xmlcalabash.model.tpl

import com.xmlcalabash.model.util.ParserConfiguration

import scala.collection.mutable.ListBuffer

class VariableBinding(override val config: ParserConfiguration,
                      override val parent: Option[Artifact],
                      val name: String,
                      val initializer: String) extends Artifact(config,parent) {

}
