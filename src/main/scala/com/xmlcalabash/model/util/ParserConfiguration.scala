package com.xmlcalabash.model.util

import com.jafpl.steps.Step
import com.xmlcalabash.config.Signatures
import net.sf.saxon.s9api.QName

trait ParserConfiguration {
  def errorListener: ErrorListener
  def stepSignatures: Signatures
  def stepImplementation(name: QName): Step
}
