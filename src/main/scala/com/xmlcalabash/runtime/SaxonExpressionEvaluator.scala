package com.xmlcalabash.runtime

import com.jafpl.runtime.ExpressionEvaluator

class SaxonExpressionEvaluator(config: SaxonRuntimeConfiguration) extends ExpressionEvaluator {
  override def value(expr: String, context: Option[Any], bindings: Option[Map[String, Any]]) = Unit
  override def booleanValue(expr: String, context: Option[Any], bindings: Option[Map[String, Any]]) = false
}
