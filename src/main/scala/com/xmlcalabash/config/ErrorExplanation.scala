package com.xmlcalabash.config

import net.sf.saxon.s9api.QName

trait ErrorExplanation {
  def message(code: QName): String
  def message(code: QName, details: List[String]): String
  def explanation(code: QName): String
  def explanation(code: QName, details: List[String]): String
}
