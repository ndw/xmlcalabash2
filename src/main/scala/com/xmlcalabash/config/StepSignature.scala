package com.xmlcalabash.config

import com.xmlcalabash.exceptions.ModelException
import net.sf.saxon.s9api.QName

import scala.collection.mutable

class StepSignature(val stepType: QName) {
  private var _inputPorts = mutable.HashMap.empty[String, PortSignature]
  private var _outputPorts = mutable.HashMap.empty[String, PortSignature]
  private var _options = mutable.HashMap.empty[QName, OptionSignature]
  private var _implementation: Option[String] = None

  def addInput(port: PortSignature): Unit = {
    if (_inputPorts.contains(port.name)) {
      throw new ModelException("dupport", s"Attempt to specify duplicate input port: ${port.name}", None)
    } else {
      _inputPorts.put(port.name, port)
    }
  }

  def addOutput(port: PortSignature): Unit = {
    if (_outputPorts.contains(port.name)) {
      throw new ModelException("dupport", s"Attempt to specify duplicate output port: ${port.name}", None)
    } else {
      _outputPorts.put(port.name, port)
    }
  }

  def addOption(opt: OptionSignature): Unit = {
    if (_options.contains(opt.name)) {
      throw new ModelException("dupopt", s"Attempt to specify duplicate option: ${opt.name}", None)
    } else {
      _options.put(opt.name, opt)
    }
  }

  def implementation_=(className: String) {
    _implementation = Some(className)
  }

  def implementation: Option[String] = _implementation

  def inputPorts: Set[String] = _inputPorts.keySet.toSet

  def outputPorts: Set[String] = _outputPorts.keySet.toSet

  def options: Set[QName] = _options.keySet.toSet

  def input(port: String): PortSignature = {
    if (_inputPorts.contains(port)) {
      _inputPorts(port)
    } else {
      throw new ModelException("badport", s"Step $stepType has no input port named $port", None)
    }
  }

  def output(port: String): PortSignature = {
    if (_outputPorts.contains(port)) {
      _outputPorts(port)
    } else {
      throw new ModelException("badport", s"Step $stepType has no output port named $port", None)
    }
  }

  def option(name: QName): OptionSignature = {
    if (_options.contains(name)) {
      _options(name)
    } else {
      throw new ModelException("badport", s"Step $stepType has no option named $name", None)
    }
  }
}

