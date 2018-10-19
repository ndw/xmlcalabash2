package com.xmlcalabash.messages

import com.jafpl.messages.ItemMessage
import com.xmlcalabash.runtime.{ExpressionContext, XProcMetadata}

class XProcItemMessage(override val item: Any,
                       override val metadata: XProcMetadata,
                       val context: ExpressionContext)
  extends ItemMessage(item, metadata)  {

  def this(item: Any, metadata: XProcMetadata) = {
    this(item, metadata, ExpressionContext.NONE)
  }
}
