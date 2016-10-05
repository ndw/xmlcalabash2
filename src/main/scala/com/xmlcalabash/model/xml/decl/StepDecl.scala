package com.xmlcalabash.model.xml.decl

import net.sf.saxon.s9api.QName

import scala.collection.mutable

/**
  * Created by ndw on 10/4/16.
  */
class StepDecl(val stepType: QName) extends Decl {
  private var _stepName: Option[String] = None
  private var _psviRequired = false
  private var _xpathVersion = "2.0"
  private var _version = "1.0"
  private var _inputs = mutable.HashMap.empty[String, InputDecl]
  private var _outputs = mutable.HashMap.empty[String, OutputDecl]
  private var _options = mutable.HashMap.empty[QName, OptionDecl]

  def this(stepType: QName, stepName: String = null) {
    this(stepType)
    if (stepName != null) {
      _stepName = Some(stepName)
    }
  }

  def stepName = _stepName

  def psviRequired = _psviRequired

  def xpathVersion = _xpathVersion

  def version = _version

  def stepName_=(value: String): Unit = {
    _stepName = Some(value)
  }

  def psviRequired_=(value: Boolean): Unit = {
    _psviRequired = value
  }

  def xpathVersion_=(value: String): Unit = {
    _xpathVersion = value
  }

  def version_=(value: String): Unit = {
    _version = value
  }

  def addInput(input: InputDecl): Unit = {
    _inputs.put(input.port, input)
  }

  def addOutput(output: OutputDecl): Unit = {
    _outputs.put(output.port, output)
  }

  def addOption(option: OptionDecl): Unit = {
    _options.put(option.name, option)
  }

  def inputs = scala.collection.immutable.HashMap.empty[String, InputDecl] ++ _inputs

  def outputs = scala.collection.immutable.HashMap.empty[String, OutputDecl] ++ _outputs

  def options = scala.collection.immutable.HashMap.empty[QName, OptionDecl] ++ _options
}

