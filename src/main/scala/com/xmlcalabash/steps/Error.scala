package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmNode}

class Error extends DefaultXmlStep {
  private val _code = new QName("", "code")

  private var _message = Option.empty[XdmNode]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode => _message = Some(node)
      case _ => logger.debug(s"p:error received unexpected item: $item")
    }
  }


  override def run(context: StaticContext) {
    val code = qnameBinding(_code).get
    throw XProcException.xcGeneralException(code, _message, context.location)
  }
}
