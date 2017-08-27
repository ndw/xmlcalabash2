package com.xmlcalabash.model.config

import com.xmlcalabash.model.exceptions.ModelException
import net.sf.saxon.s9api.QName

import scala.collection.mutable

class Signatures {
  private val _steps = mutable.HashMap.empty[QName, StepSignature]

  def addStep(step: StepSignature): Unit = {
    if (_steps.contains(step.stepType)) {
      throw new ModelException("dupstep", s"Duplicate step type: ${step.stepType}")
    }
    _steps.put(step.stepType, step)
  }

  def stepTypes: Set[QName] = _steps.keySet.toSet
  def step(stepType: QName): StepSignature = _steps(stepType)
}
