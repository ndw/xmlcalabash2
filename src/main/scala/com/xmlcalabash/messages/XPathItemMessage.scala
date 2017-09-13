package com.xmlcalabash.messages

import com.jafpl.messages.ItemMessage
import com.xmlcalabash.runtime.{ExpressionContext, XProcMetadata}
import net.sf.saxon.s9api.XdmItem

class XPathItemMessage(override val item: XdmItem,
                       override val metadata: XProcMetadata,
                       val context: ExpressionContext)
  extends ItemMessage(item, metadata) {
}
