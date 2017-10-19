package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.graph.Location

object ExpressionContext {
  private val _none = new ExpressionContext(None, Map.empty[String,String], None)
  def NONE: ExpressionContext = _none
}

class ExpressionContext(val baseURI: Option[URI],
                        val nsBindings: Map[String,String],
                        val location: Option[Location]) {
  def this(nsBindings: Map[String,String]) {
    this(None, nsBindings, None)
  }

  def this(uri: URI, nsBindings: Map[String,String]) {
    this(Some(uri), nsBindings, None)
  }

  def this(uri: URI, nsBindings: Map[String,String], location: Location) {
    this(Some(uri), nsBindings, Some(location))
  }
}
