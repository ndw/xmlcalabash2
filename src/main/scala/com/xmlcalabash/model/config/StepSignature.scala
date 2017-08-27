package com.xmlcalabash.model.config

import com.xmlcalabash.model.exceptions.ModelException
import net.sf.saxon.s9api.QName

import scala.collection.mutable

class StepSignature(val stepType: QName) {
  private val _inputs = mutable.HashMap.empty[String,PortSignature]
  private val _outputs = mutable.HashMap.empty[String,PortSignature]
  private val _opts = mutable.HashMap.empty[String,OptionSignature]

  def addInput(port: PortSignature): Unit = {
    if (_inputs.contains(port.name)) {
      throw new ModelException("dupinput", s"Duplicate input port: $port")
    }
    _inputs.put(port.name, port)
  }

  def addOutput(port: PortSignature): Unit = {
    if (_outputs.contains(port.name)) {
      throw new ModelException("dupinput", s"Duplicate output port: $port")
    }
    _outputs.put(port.name, port)
  }

  def addOption(opt: OptionSignature): Unit = {
    if (_opts.contains(opt.name)) {
      throw new ModelException("dupinput", s"Duplicate option port: $opt")
    }
    _opts.put(opt.name, opt)
  }

  def inputPorts: Set[String] = _inputs.keySet.toSet
  def outputPorts: Set[String] = _outputs.keySet.toSet
  def optionNames: Set[String] = _opts.keySet.toSet

  def input(port: String): PortSignature = _inputs(port)
  def output(port: String): PortSignature = _outputs(port)
  def option(name: String): OptionSignature = _opts(name)
}
