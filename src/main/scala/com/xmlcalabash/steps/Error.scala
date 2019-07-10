package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.StepException
import com.xmlcalabash.runtime.{StaticContext, XmlPortSpecification}
import com.xmlcalabash.util.StepErrors
import net.sf.saxon.s9api.QName
import net.sf.saxon.value.QNameValue

class Error extends DefaultXmlStep {
  private val _code = new QName("", "code")
  private val _code_prefix = new QName("", "code-prefix")
  private val _code_namespace = new QName("", "code-namespace")
  private val _message = new QName("", "message")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def run(context: StaticContext) {
    val qn = bindings(_code).value.getUnderlyingValue.asInstanceOf[QNameValue]
    val code = new QName(qn.getPrefix, qn.getNamespaceURI, qn.getLocalName)

    val errors = new StepErrors(config)
    val errout = if (bindings.contains(_message)) {
      errors.error(code, bindings(_message).getStringValue)
    } else {
      errors.error(code)
    }

    throw new StepException(code, bindings(_code).context, errout)
  }
}
