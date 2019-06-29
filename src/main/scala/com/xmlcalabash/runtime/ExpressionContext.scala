package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.graph.Location

object ExpressionContext {
  private val _none = new ExpressionContext(new StaticContext())
  def NONE: ExpressionContext = _none
}

class ExpressionContext(val staticContext: StaticContext, val options: Option[SaxonExpressionOptions]) {
  def this(staticContext: StaticContext) {
    this(staticContext, None)
  }
  def this(context: ExpressionContext, options: SaxonExpressionOptions) {
    this(context.staticContext, Some(options))
  }
  def baseURI: Option[URI] = staticContext.baseURI
  def location: Option[Location] = staticContext.location
  def nsBindings: Map[String,String] = staticContext.inScopeNS
}
