package com.xmlcalabash.model.util

import com.jafpl.steps.Step
import com.xmlcalabash.model.config.{PortSignature, Signatures, StepSignature}
import com.xmlcalabash.model.exceptions.ModelException
import com.xmlcalabash.model.xml.{ParserConfiguration, XProcConstants}
import com.xmlcalabash.steps.{Identity, Producer, Sink}
import net.sf.saxon.s9api.QName

class DefaultParserConfiguration extends ParserConfiguration {
  private val _errorListener = new DefaultErrorListener()
  private val _signatures = new Signatures()

  var sig = new StepSignature(XProcConstants.p_identity)
  sig.addInput(new PortSignature("source", primary=true, sequence=true))
  sig.addOutput(new PortSignature("result", primary=true, sequence=true))
  _signatures.addStep(sig)

  sig = new StepSignature(XProcConstants.p_producer)
  sig.addOutput(new PortSignature("result", primary=true, sequence=true))
  _signatures.addStep(sig)

  sig = new StepSignature(XProcConstants.p_sink)
  sig.addInput(new PortSignature("source", primary=true, sequence=true))
  _signatures.addStep(sig)

  override def errorListener: ErrorListener = _errorListener

  override def stepSignatures: Signatures = _signatures

  override def stepImplementation(stepType: QName): Step = {
    stepType match {
      case XProcConstants.p_identity => new Identity()
      case XProcConstants.p_sink => new Sink()
      case XProcConstants.p_producer => new Producer()
      case _ => throw new ModelException("badtype", "Unexpected step type: $stepType")
    }
  }

}
