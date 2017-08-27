package com.xmlcalabash.runtime

import com.jafpl.runtime.{ExpressionEvaluator, RuntimeConfiguration}
import net.sf.saxon.s9api.Processor

class SaxonRuntimeConfiguration(val processor: Processor) extends RuntimeConfiguration {
  private val _expressionEvaluator = new SaxonExpressionEvaluator(this)

  override def expressionEvaluator(): ExpressionEvaluator = _expressionEvaluator

  override def traceEnabled(trace: String): Boolean = {
    false
  }

  override def watchdogTimeout = 1000L
}
