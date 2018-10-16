package com.xmlcalabash.model.tpl

import com.xmlcalabash.config.XMLCalabashConfig

import scala.collection.mutable.ListBuffer

class Step(override val config: XMLCalabashConfig, override val parent: Option[Artifact]) extends Artifact(config,parent) {
  private var _sourceBindings = ListBuffer.empty[PortBinding]
  private var _resultBindings = ListBuffer.empty[PortBinding]
  private var _optionBindings = ListBuffer.empty[OptionBinding]

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

  def optionBindings: List[OptionBinding] = _optionBindings.toList
  protected[tpl] def optionBindings_=(bindings: List[OptionBinding]): Unit = {
    _optionBindings.clear()
    for (binding <- bindings) {
      _optionBindings += binding
    }
  }

  protected[tpl] def addSourceBinding(binding: PortBinding): Unit = {
    _sourceBindings += binding
  }

  protected[tpl] def addResultBinding(binding: PortBinding): Unit = {
    _resultBindings += binding
  }

  protected[tpl] def addOptionBinding(binding: OptionBinding): Unit = {
    _optionBindings += binding
  }
}
