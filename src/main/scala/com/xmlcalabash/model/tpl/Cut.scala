package com.xmlcalabash.model.tpl

import com.xmlcalabash.model.util.ParserConfiguration

import scala.collection.mutable.ListBuffer

class Cut(override val config: ParserConfiguration, override val parent: Option[Artifact]) extends Artifact(config,parent) {
  private val _stepSeq = ListBuffer.empty[Step]

  def stepSequence: List[Step] = _stepSeq.toList

  def addStep(step: Step): Unit = {
    _stepSeq += step
  }

}
