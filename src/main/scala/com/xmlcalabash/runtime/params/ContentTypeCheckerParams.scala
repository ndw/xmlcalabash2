package com.xmlcalabash.runtime.params

import com.xmlcalabash.runtime.{ImplParams, StaticContext}
import com.xmlcalabash.util.MediaType

class ContentTypeCheckerParams(val port: String,
                               val contentTypes: List[MediaType],
                               val context: StaticContext,
                               val select: Option[String],
                               val inputPort: Boolean,
                               val sequence: Boolean) extends ImplParams {
}
