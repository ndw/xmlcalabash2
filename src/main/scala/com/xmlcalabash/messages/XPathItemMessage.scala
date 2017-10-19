package com.xmlcalabash.messages

import com.jafpl.messages.ItemMessage
import com.xmlcalabash.runtime.{ExpressionContext, XProcMetadata}
import net.sf.saxon.s9api.XdmValue

class XPathItemMessage(override val item: XdmValue,
                       override val metadata: XProcMetadata,
                       val context: ExpressionContext)
  extends ItemMessage(item, metadata) {
}
