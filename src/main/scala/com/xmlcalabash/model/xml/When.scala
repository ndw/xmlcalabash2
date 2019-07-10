package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ExprParams, XProcXPathExpression}
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.XdmNode

class When(override val config: XMLCalabashConfig) extends ChooseBranch(config) {

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    _collection = staticContext.parseBoolean(attr(XProcConstants._collection))

    if (attributes.contains(XProcConstants._test)) {
      _test = attr(XProcConstants._test).get
      val params = new ExprParams(collection)
      testExpr = new XProcXPathExpression(staticContext, _test, None, None, Some(params))
    } else {
      throw XProcException.xsMissingRequiredAttribute(XProcConstants._test, location)
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startWhen(tumble_id, stepName, _test)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endWhen()
  }

  override def toString: String = {
    s"p:when $stepName"
  }
}
