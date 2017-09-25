package com.xmlcalabash.util

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.ExpressionContext
import net.sf.saxon.s9api.{ItemTypeFactory, QName, XdmAtomicValue}

class TypeUtils(val config: XMLCalabash) {

  def castAs(value: XdmAtomicValue, xsdtype: Option[QName], context: ExpressionContext): XdmAtomicValue = {
    if (xsdtype.isEmpty) {
      return value
    }

    if ((xsdtype.get == XProcConstants.xs_untypedAtomic) || (xsdtype.get == XProcConstants.xs_string)) {
      return value
    }

    if (xsdtype.get == XProcConstants.xs_QName) {
      return new XdmAtomicValue(ValueParser.parseQName(value.getStringValue, context.nsBindings))
    }

    val typeFactory = new ItemTypeFactory(config.processor)
    val itype = typeFactory.getAtomicType(xsdtype.get)

    new XdmAtomicValue(value.getStringValue, itype)
  }
}
