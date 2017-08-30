package com.xmlcalabash.config

import scala.collection.mutable

class StepConfig(val name: String) {
  private var _inputPorts = mutable.ListBuffer.empty[PortConfig]
  private var _outputPorts = mutable.ListBuffer.empty[PortConfig]
  private var _options = mutable.ListBuffer.empty[OptionConfig]
  private var _implementation: Option[String] = None

  def inputPorts: List[PortConfig] = _inputPorts.toList
  protected[config] def addInputPort(port: PortConfig): Unit = {
    _inputPorts += port
  }

  def outputPorts: List[PortConfig] = _outputPorts.toList
  protected[config] def addOutputPort(port: PortConfig): Unit = {
    _outputPorts += port
  }

  def options: List[OptionConfig] = _options.toList
  protected[config] def addOption(opt: OptionConfig): Unit = {
    _options += opt
  }

  def implementation: Option[String] = _implementation
  protected[config] def implementation_=(className: String) {
    _implementation = Some(className)
  }

}
