package com.xmlcalabash.model.xml.decl

import com.xmlcalabash.core.XProcConstants
import net.sf.saxon.s9api.QName

import scala.collection.mutable

/**
  * Created by ndw on 10/4/16.
  */
class StepLibrary {
  protected val _steps = mutable.HashMap.empty[QName, StepDecl]
  private val noOptions = List.empty[OptionDecl]

  def addDecl(stepType: QName, inputs: List[InputDecl], outputs: List[OutputDecl]): Unit = {
    addDecl(stepType, inputs, outputs, noOptions)
  }

  def addDecl(stepType: QName, inputs: List[InputDecl], outputs: List[OutputDecl], options: List[OptionDecl]): Unit = {
    // Compute primary
    var primary: Option[Boolean] = None
    var count = 0
    for (input <- inputs) {
      if (input._primary.isDefined) {
        primary = input._primary
      }
      count += 1
    }
    if (primary.isEmpty && count == 1) {
      inputs.head.primary = true
    }

    primary = None
    count = 0
    for (output <- outputs) {
      if (output._primary.isDefined) {
        primary = output._primary
      }
      count += 1
    }
    if (primary.isEmpty && count == 1) {
      outputs.head.primary = true
    }

    val decl = new StepDecl(stepType)
    inputs.foreach { decl.addInput }
    outputs.foreach { decl.addOutput }
    options.foreach { decl.addOption }
    _steps.put(stepType, decl)
  }

  def steps = scala.collection.immutable.HashMap.empty[QName, StepDecl] ++ _steps
}
