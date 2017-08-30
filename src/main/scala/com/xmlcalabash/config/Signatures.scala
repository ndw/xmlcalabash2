package com.xmlcalabash.config

import net.sf.saxon.s9api.QName
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class Signatures {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val _steps = mutable.HashMap.empty[QName, StepSignature]

  def addStep(step: StepSignature): Unit = {
    if (_steps.contains(step.stepType)) {
      logger.warn(s"Duplicate definition of ${step.stepType}")
    }
    _steps.put(step.stepType, step)
  }

  def stepTypes: Set[QName] = _steps.keySet.toSet
  def step(stepType: QName): StepSignature = _steps(stepType)
}
