package com.xmlcalabash.messages

import com.xmlcalabash.runtime.{ExpressionContext, XProcMetadata}
import net.sf.saxon.s9api.XdmNode

class XdmNodeItemMessage(override val item: XdmNode,
                         override val metadata: XProcMetadata,
                         override val context: ExpressionContext)
  extends XdmValueItemMessage(item, metadata, context) {

  def this(item: XdmNode, metadata: XProcMetadata) = {
    this(item, metadata, ExpressionContext.NONE)
  }
}
