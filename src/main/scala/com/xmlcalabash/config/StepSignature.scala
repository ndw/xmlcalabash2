package com.xmlcalabash.config

import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class StepSignature(val stepType: QName) {
  private var _inputPorts = mutable.HashMap.empty[String, PortSignature]
  private var _outputPorts = mutable.HashMap.empty[String, PortSignature]
  private var _options = mutable.HashMap.empty[QName, OptionSignature]
  private var _implementation = ListBuffer.empty[String]

  def addInput(port: PortSignature, location: Location): Unit = {
    if (_inputPorts.contains(port.name)) {
      throw new ModelException(ExceptionCode.DUPINPUTSIG, port.name, location)
    } else {
      _inputPorts.put(port.name, port)
    }
  }

  def addOutput(port: PortSignature, location: Location): Unit = {
    if (_outputPorts.contains(port.name)) {
      throw new ModelException(ExceptionCode.DUPOUTPUTSIG, port.name, location)
    } else {
      _outputPorts.put(port.name, port)
    }
  }

  def addOption(opt: OptionSignature, location: Location): Unit = {
    if (_options.contains(opt.name)) {
      throw new ModelException(ExceptionCode.DUPOPTSIG, opt.name.toString, location)
    } else {
      _options.put(opt.name, opt)
    }
  }

  def implementation_=(className: String) {
    _implementation += className
  }

  def implementation: List[String] = _implementation.toList

  def inputPorts: Set[String] = _inputPorts.keySet.toSet

  def outputPorts: Set[String] = _outputPorts.keySet.toSet

  def options: Set[QName] = _options.keySet.toSet

  def input(port: String, location: Location): PortSignature = {
    if (_inputPorts.contains(port)) {
      _inputPorts(port)
    } else {
      throw new ModelException(ExceptionCode.BADINPUTSIG, List(stepType.toString, port), location)
    }
  }

  def output(port: String, location: Location): PortSignature = {
    if (_outputPorts.contains(port)) {
      _outputPorts(port)
    } else {
      throw new ModelException(ExceptionCode.BADOUTPUTSIG, List(stepType.toString, port), location)
    }
  }

  def option(name: QName, location: Location): OptionSignature = {
    if (_options.contains(name)) {
      _options(name)
    } else {
      throw new ModelException(ExceptionCode.BADOPTSIG, List(stepType.toString, name.toString), location)
    }
  }
}

