package com.xmlcalabash.model.tpl.containers

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.tpl.{Artifact, Cut, Step, VariableBinding}

import scala.collection.mutable.ListBuffer

class Container(override val config: XMLCalabashConfig, override val parent: Option[Artifact]) extends Step(config,parent) {
  protected val _children = ListBuffer.empty[Artifact]

  def children: List[Artifact] = _children.toList

  protected[tpl] def addCut(cut: Cut): Unit = {
    _children += cut
  }

  protected[tpl] def addVariableBinding(binding: VariableBinding): Unit = {
    _children += binding
  }
}
