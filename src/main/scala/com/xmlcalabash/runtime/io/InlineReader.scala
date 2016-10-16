package com.xmlcalabash.runtime.io

import com.jafpl.runtime.DefaultStep
import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.items.XPathDataModelItem
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/10/16.
  */
class InlineReader(engine: XProcEngine, doc: XdmNode) extends DefaultStep {
  label = "_inline_reader"

  override def run(): Unit = {
    super.run()
    controller.send("result", new XPathDataModelItem(doc))
  }
}
