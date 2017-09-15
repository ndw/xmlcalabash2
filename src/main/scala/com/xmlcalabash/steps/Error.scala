package com.xmlcalabash.steps

import com.jafpl.exceptions.PipelineException
import com.xmlcalabash.exceptions.StepException
import com.xmlcalabash.model.util.ValueParser
import com.xmlcalabash.runtime.XmlPortSpecification
import net.sf.saxon.s9api.QName

class Error extends DefaultXmlStep {
  private val _code = new QName("", "code")
  private val _code_prefix = new QName("", "code-prefix")
  private val _code_namespace = new QName("", "code-namespace")
  private val _message = new QName("", "message")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.NONE

  override def run() {
    var code = new QName("", "test")
    val name = bindings(_code).value.getStringValue
    if (bindings.contains(_code_prefix) || bindings.contains(_code_namespace)) {
      if (name.contains(":")) {
        throw StepException.dynamicError(34)
      }
      if (bindings.contains(_code_prefix)) {
        code = new QName(bindings(_code_prefix).value.getStringValue, bindings(_code_namespace).value.getStringValue, name)
      } else {
        code = new QName(bindings(_code_namespace).value.getStringValue, name)
      }
    } else {
      code = ValueParser.parseQName(name, bindings(_code).context.nsBindings)
    }

    if (bindings.contains(_message)) {
      throw new StepException(code, bindings(_message).value.getStringValue, bindings(_message).context)
    } else {
      throw new StepException(code, bindings(_code).context)
    }
  }
}