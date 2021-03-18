package com.xmlcalabash.config

import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.xml.DeclareStep
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class StepSignature(val stepType: Option[QName]) {
  private var _inputPorts = mutable.HashMap.empty[String, PortSignature]
  private var _outputPorts = mutable.HashMap.empty[String, PortSignature]
  private var _options = mutable.HashMap.empty[QName, OptionSignature]
  private var _implementation = ListBuffer.empty[String]
  private var _declaration = Option.empty[DeclareStep]

  def addInput(port: PortSignature, location: Location): Unit = {
    if (_inputPorts.contains(port.port)) {
      throw new ModelException(ExceptionCode.DUPINPUTSIG, port.port, location)
    } else {
      _inputPorts.put(port.port, port)
    }
  }

  def addOutput(port: PortSignature, location: Location): Unit = {
    if (_outputPorts.contains(port.port)) {
      throw new ModelException(ExceptionCode.DUPOUTPUTSIG, port.port, location)
    } else {
      _outputPorts.put(port.port, port)
    }
  }

  def addOption(opt: OptionSignature, location: Location): Unit = {
    if (_options.contains(opt.name)) {
      throw new ModelException(ExceptionCode.DUPOPTSIG, opt.name.toString, location)
    } else {
      _options.put(opt.name, opt)
    }
  }

  def implementation_=(className: String): Unit = {
    if (_declaration.isDefined) {
      throw new RuntimeException("Cannot have an atomic step with the same name as a non-atomic step")
    }
    _implementation += className
  }

  def declaration: Option[DeclareStep] = _declaration
  def declaration_=(decl: DeclareStep): Unit = {
    if (_implementation.nonEmpty) {
      throw new RuntimeException("Cannot have an atomic step with the same name as a non-atomic step")
    }
    if (_declaration.isDefined) {
      throw new RuntimeException("Cannot redefine a step")
    }
    _declaration = Some(decl)
  }

  def implementation: List[String] = _implementation.toList

  def inputPorts: Set[String] = _inputPorts.keySet.toSet
  def inputs: Set[PortSignature] = _inputPorts.values.toSet

  def outputPorts: Set[String] = _outputPorts.keySet.toSet
  def outputs: Set[PortSignature] = _outputPorts.values.toSet

  def optionNames: Set[QName] = _options.keySet.toSet
  def options: Set[OptionSignature] = _options.values.toSet

  def input(port: String, location: Option[Location]): PortSignature = {
    if (_inputPorts.contains(port)) {
      _inputPorts(port)
    } else {
      throw new ModelException(ExceptionCode.BADINPUTSIG, List(stepType.toString, port), location)
    }
  }

  def output(port: String, location: Option[Location]): PortSignature = {
    if (_outputPorts.contains(port)) {
      _outputPorts(port)
    } else {
      throw new ModelException(ExceptionCode.BADOUTPUTSIG, List(stepType.toString, port), location)
    }
  }

  def option(name: QName, location: Option[Location]): OptionSignature = {
    if (_options.contains(name)) {
      _options(name)
    } else {
      throw new ModelException(ExceptionCode.BADOPTSIG, List(stepType.toString, name.toString), location)
    }
  }

  def primaryInput: Option[PortSignature] = {
    for ((name, sig) <- _inputPorts) {
      if (sig.primary) {
        return Some(sig)
      }
    }
    None
  }

  def primaryOutput: Option[PortSignature] = {
    for ((name, sig) <- _outputPorts) {
      if (sig.primary) {
        return Some(sig)
      }
    }
    None
  }

  override def toString: String = {
    stepType.toString
  }
}

