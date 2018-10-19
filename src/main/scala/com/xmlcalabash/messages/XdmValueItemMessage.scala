package com.xmlcalabash.messages

import com.xmlcalabash.runtime.{ExpressionContext, XProcMetadata}
import net.sf.saxon.s9api.XdmValue

class XdmValueItemMessage(override val item: XdmValue,
                          override val metadata: XProcMetadata,
                          override val context: ExpressionContext)
  extends XProcItemMessage(item, metadata, context) {

  def this(item: XdmValue, metadata: XProcMetadata) = {
    this(item, metadata, ExpressionContext.NONE)
  }
}
