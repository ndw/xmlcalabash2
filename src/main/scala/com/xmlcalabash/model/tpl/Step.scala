package com.xmlcalabash.model.tpl

import com.xmlcalabash.model.util.ParserConfiguration

import scala.collection.mutable.ListBuffer

class Step(override val config: ParserConfiguration, override val parent: Option[Artifact]) extends Artifact(config,parent) {
  private var _sourceBindings = ListBuffer.empty[PortBinding]
  private var _resultBindings = ListBuffer.empty[PortBinding]

  def sourceBindings: List[PortBinding] = _sourceBindings.toList
  protected[tpl] def sourceBindings_=(bindings: List[PortBinding]): Unit = {
    _sourceBindings.clear()
    for (binding <- bindings) {
      _sourceBindings += binding
    }
  }

  def resultBindings: List[PortBinding] = _resultBindings.toList
  protected[tpl] def resultBindings_=(bindings: List[PortBinding]): Unit = {
    _resultBindings.clear()
    for (binding <- bindings) {
      _resultBindings += binding
    }
  }

  protected[tpl] def addSourceBinding(binding: PortBinding): Unit = {
    _sourceBindings += binding
  }

  protected[tpl] def addResultBinding(binding: PortBinding): Unit = {
    _resultBindings += binding
  }

}
