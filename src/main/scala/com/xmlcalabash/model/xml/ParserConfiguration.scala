package com.xmlcalabash.model.xml

import com.jafpl.steps.Step
import com.xmlcalabash.model.config.Signatures
import com.xmlcalabash.model.util.ErrorListener
import net.sf.saxon.s9api.QName

trait ParserConfiguration {
  def errorListener: ErrorListener
  def stepSignatures: Signatures
  def stepImplementation(name: QName): Step
}
