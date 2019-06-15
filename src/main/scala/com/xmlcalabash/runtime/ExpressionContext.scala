package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.graph.Location

object ExpressionContext {
  private val _none = new ExpressionContext(StaticContext.EMPTY)
  def NONE: ExpressionContext = _none
}

class ExpressionContext(val staticContext: StaticContext) {

  def baseURI: Option[URI] = staticContext.baseURI
  def location: Option[Location] = staticContext.location
  def nsBindings: Map[String,String] = staticContext.inScopeNS
}
