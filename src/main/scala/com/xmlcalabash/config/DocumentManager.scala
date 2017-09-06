package com.xmlcalabash.config

import java.net.URI

import net.sf.saxon.s9api.XdmNode
import org.xml.sax.InputSource

trait DocumentManager {
  def parse(uri: URI): XdmNode
  def parse(href: String): XdmNode
  def parse(href: String, base: String): XdmNode
  def parse(href: String, base: String, dtdValidate: Boolean): XdmNode
  def parse(isource: InputSource): XdmNode
}
