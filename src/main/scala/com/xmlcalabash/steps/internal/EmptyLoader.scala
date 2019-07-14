package com.xmlcalabash.steps.internal

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.runtime.params.EmptyLoaderParams
import com.xmlcalabash.runtime.{ImplParams, StaticContext, XmlPortSpecification}

// N.B. This looks like a step, but it isn't really. It gets passed all of the variable bindings
// and the context item and it evaluates its "options" directly. This is necessary because in
// the case where this is a default binding, it must *not* evaluate its options if the default
// is not used.

class EmptyLoader() extends AbstractLoader {
  override def inputSpec: XmlPortSpecification = {
    XmlPortSpecification.NONE
  }
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def configure(config: XMLCalabashConfig, params: Option[ImplParams]): Unit = {
    if (params.isEmpty) {
      throw new RuntimeException("empty loader params required")
    }

    params.get match {
      case doc: EmptyLoaderParams =>
        exprContext = doc.context
      case _ =>
        throw new RuntimeException("document loader params wrong type")
    }
  }

  override def run(context: StaticContext): Unit = {
    if (disabled) {
      return
    }

    super.run(context)
    // output nothing
  }
}
