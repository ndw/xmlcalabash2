package com.xmlcalabash.config

import net.sf.saxon.Configuration

trait SaxonConfigurer {
  def configureSchematron(config: Configuration)
  def configureXSD(config: Configuration)
  def configureXQuery(config: Configuration)
  def configureXSLT(config: Configuration)
}
