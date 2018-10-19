package com.xmlcalabash.messages

import com.xmlcalabash.runtime.{ExpressionContext, XProcMetadata}
import net.sf.saxon.s9api.XdmNode

class AnyItemMessage(override val item: XdmNode,
                     shadowValue: Any,
                     override val metadata: XProcMetadata,
                     override val context: ExpressionContext) extends XProcItemMessage(item, metadata, context) {
    def shadow: Any = shadowValue

    def this(item: XdmNode, shadow: Any, metadata: XProcMetadata) = {
        this(item, shadow, metadata, ExpressionContext.NONE)
    }
}
